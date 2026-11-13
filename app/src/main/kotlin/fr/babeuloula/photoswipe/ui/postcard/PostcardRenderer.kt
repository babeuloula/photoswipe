package fr.babeuloula.photoswipe.ui.postcard

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.ui.theme.LightColorScheme
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Couleurs et polices de la carte postale (PS-177) : celles du thème clair, palette ambre, quel
 * que soit le thème de l'application, pour que la carte soit la même pour tous ceux qui la reçoivent.
 *
 * @property paper fond de la carte, autour de la photo.
 * @property edge brun du repère de bord de film.
 * @property edgeText ambre du texte du repère.
 * @property brandText couleur de la mention « PhotoSwipe ».
 * @property mono police du repère (IBM Plex Mono).
 * @property brandFont police de la mention (Atkinson Hyperlegible Next).
 */
data class PostcardStyle(
    val paper: Int,
    val edge: Int,
    val edgeText: Int,
    val brandText: Int,
    val mono: Typeface,
    val brandFont: Typeface,
) {
    companion object {
        /** Style tiré du thème clair et des polices embarquées de [context]. */
        fun of(context: Context): PostcardStyle =
            PostcardStyle(
                paper = LightColorScheme.surface.toArgb(),
                edge = LightColorScheme.onPrimaryFixed.toArgb(),
                edgeText = LightColorScheme.primaryFixedDim.toArgb(),
                brandText = LightColorScheme.onSurfaceVariant.toArgb(),
                mono = font(context, R.font.ibm_plex_mono_medium, Typeface.MONOSPACE),
                brandFont =
                    Typeface.create(
                        font(context, R.font.atkinson_hyperlegible_next, Typeface.DEFAULT),
                        BRAND_WEIGHT,
                        false,
                    ),
            )

        /** Police embarquée [id], ou [fallback] si elle ne peut pas être lue. */
        private fun font(
            context: Context,
            id: Int,
            fallback: Typeface,
        ): Typeface = runCatching { ResourcesCompat.getFont(context, id) }.getOrNull() ?: fallback
    }
}

/**
 * Place des éléments sur la carte postale, en pixels : la photo en haut, dans un cadre de papier,
 * et le pied de carte (repère, mention) dessous. La photo a toujours [PHOTO_LONG_SIDE] pixels sur
 * son grand côté : la taille de sortie ne dépend que des proportions, bornées entre [MIN_RATIO] et
 * [MAX_RATIO] ; au-delà (panorama, capture d'écran très haute), la photo est recadrée au centre.
 *
 * @property width largeur de la carte.
 * @property height hauteur de la carte.
 * @property photo emplacement de la photo.
 * @property footer bande du bas, où se posent le repère et la mention.
 */
data class PostcardLayout(
    val width: Int,
    val height: Int,
    val photo: Rect,
    val footer: Rect,
) {
    /** Marge intérieure du cadre, autour de la photo et du pied de carte. */
    val margin: Int get() = photo.left

    companion object {
        /** Mise en page d'une photo de [photoWidth] × [photoHeight] pixels (ou de ses proportions). */
        fun of(
            photoWidth: Int,
            photoHeight: Int,
        ): PostcardLayout {
            val ratio = postcardRatio(photoWidth, photoHeight)
            val photoW = if (ratio >= 1f) PHOTO_LONG_SIDE else (PHOTO_LONG_SIDE * ratio).roundToInt()
            val photoH = if (ratio >= 1f) (PHOTO_LONG_SIDE / ratio).roundToInt() else PHOTO_LONG_SIDE
            val margin = (UNIT * MARGIN_UNITS).roundToInt()
            val footerHeight = (UNIT * FOOTER_UNITS).roundToInt()
            val width = photoW + 2 * margin
            val photo = Rect(margin, margin, margin + photoW, margin + photoH)
            val footer = Rect(margin, photo.bottom, width - margin, photo.bottom + footerHeight)
            return PostcardLayout(width, footer.bottom, photo, footer)
        }
    }
}

/** Proportions largeur / hauteur gardées pour une photo de [width] × [height], bornées. */
internal fun postcardRatio(
    width: Int,
    height: Int,
): Float = (width.toFloat() / max(1, height)).coerceIn(MIN_RATIO, MAX_RATIO)

/** Partie de la photo de [width] × [height] gardée sur la carte : toute, ou recadrée au centre. */
internal fun postcardCrop(
    width: Int,
    height: Int,
): Rect {
    val ratio = postcardRatio(width, height)
    val cropW = min(width, (height * ratio).roundToInt())
    val cropH = min(height, (width / ratio).roundToInt())
    val left = (width - cropW) / 2
    val top = (height - cropH) / 2
    return Rect(left, top, left + cropW, top + cropH)
}

/**
 * Lit la photo [uri] à taille bornée : juste assez pour remplir la carte, sans jamais l'agrandir,
 * pour qu'une photo de 50 mégapixels ne sature pas la mémoire. L'orientation EXIF est appliquée.
 * La photo d'origine n'est que lue.
 */
fun decodePostcardPhoto(
    resolver: ContentResolver,
    uri: Uri,
): Bitmap =
    ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
        // Dessinée sur un Canvas logiciel : une image en mémoire graphique ne s'y dessine pas.
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        val crop = postcardCrop(info.size.width, info.size.height)
        val scale = min(1f, PHOTO_LONG_SIDE.toFloat() / max(crop.width(), crop.height()))
        decoder.setTargetSize(
            max(1, (info.size.width * scale).roundToInt()),
            max(1, (info.size.height * scale).roundToInt()),
        )
    }

/**
 * Compose la carte postale de [photo] (PS-177) : la photo dans un cadre de papier, avec sous elle
 * le repère de bord de film [label] (« Il y a 5 ans · 6 nov. 2021 ») et, si [brand] n'est pas nul,
 * la mention « PhotoSwipe » à droite.
 */
fun renderPostcard(
    photo: Bitmap,
    label: String,
    brand: String?,
    style: PostcardStyle,
): Bitmap {
    val layout = PostcardLayout.of(photo.width, photo.height)
    val card = Bitmap.createBitmap(layout.width, layout.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(card)
    canvas.drawColor(style.paper)
    drawPhoto(canvas, photo, layout.photo)
    drawFooter(canvas, layout.footer, label, brand, style)
    return card
}

/** Photo recadrée si besoin, aux coins légèrement arrondis comme un tirage. */
private fun drawPhoto(
    canvas: Canvas,
    photo: Bitmap,
    target: Rect,
) {
    val radius = UNIT * PHOTO_CORNER_UNITS
    canvas.save()
    canvas.clipPath(Path().apply { addRoundRect(RectF(target), radius, radius, Path.Direction.CW) })
    canvas.drawBitmap(photo, postcardCrop(photo.width, photo.height), target, Paint(Paint.FILTER_BITMAP_FLAG))
    canvas.restore()
}

/**
 * Pied de carte : le repère à gauche, la mention à droite. Si les deux ne tiennent pas sur la
 * largeur (photo très haute), leur texte rapetisse jusqu'à tenir.
 */
private fun drawFooter(
    canvas: Canvas,
    footer: Rect,
    label: String,
    brand: String?,
    style: PostcardStyle,
) {
    val labelPaint = textPaint(style.mono, style.edgeText, UNIT * LABEL_TEXT_UNITS)
    val brandPaint = textPaint(style.brandFont, style.brandText, UNIT * BRAND_TEXT_UNITS)
    val chipPadding = UNIT * CHIP_PADDING_UNITS
    val gap = UNIT * GAP_UNITS
    val needed =
        labelPaint.measureText(label) + 2 * chipPadding + (brand?.let { gap + brandPaint.measureText(it) } ?: 0f)
    val scale = min(1f, footer.width() / needed)
    labelPaint.textSize *= scale
    brandPaint.textSize *= scale
    val chipHeight = UNIT * CHIP_HEIGHT_UNITS * scale
    val chip =
        RectF(
            footer.left.toFloat(),
            footer.exactCenterY() - chipHeight / 2,
            footer.left + labelPaint.measureText(label) + 2 * chipPadding * scale,
            footer.exactCenterY() + chipHeight / 2,
        )
    canvas.drawPath(filmEdgePath(chip), Paint(Paint.ANTI_ALIAS_FLAG).apply { color = style.edge })
    canvas.drawText(label, chip.left + chipPadding * scale, baseline(chip.centerY(), labelPaint), labelPaint)
    if (brand != null) {
        brandPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(brand, footer.right.toFloat(), baseline(chip.centerY(), brandPaint), brandPaint)
    }
}

/** Contour du repère de bord de film, comme `FilmEdgeShape` : coins arrondis, coin haut de fin coupé. */
private fun filmEdgePath(chip: RectF): Path {
    val radius = chip.height() * CHIP_CORNER_RATIO
    val cut = chip.height() * CHIP_CUT_RATIO
    return Path().apply {
        moveTo(chip.left + radius, chip.top)
        lineTo(chip.right - cut, chip.top)
        lineTo(chip.right, chip.top + cut)
        lineTo(chip.right, chip.bottom - radius)
        quadTo(chip.right, chip.bottom, chip.right - radius, chip.bottom)
        lineTo(chip.left + radius, chip.bottom)
        quadTo(chip.left, chip.bottom, chip.left, chip.bottom - radius)
        lineTo(chip.left, chip.top + radius)
        quadTo(chip.left, chip.top, chip.left + radius, chip.top)
        close()
    }
}

/** Pinceau de texte lissé. */
private fun textPaint(
    typeface: Typeface,
    color: Int,
    size: Float,
): Paint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        this.color = color
        textSize = size
    }

/** Ligne de base qui centre verticalement le texte de [paint] sur [centerY]. */
private fun baseline(
    centerY: Float,
    paint: Paint,
): Float = centerY - (paint.descent() + paint.ascent()) / 2

/** Grand côté de la photo sur la carte, en pixels : assez pour un écran, sans saturer la mémoire. */
const val PHOTO_LONG_SIDE = 1440

/** Proportions les plus hautes gardées (9:16) ; au-delà, la photo est recadrée. */
const val MIN_RATIO = 9f / 16f

/** Proportions les plus larges gardées (2:1, panorama) ; au-delà, la photo est recadrée. */
const val MAX_RATIO = 2f

/** Unité de la mise en page : 1/40 du grand côté de la photo. */
private const val UNIT = PHOTO_LONG_SIDE / 40f
private const val MARGIN_UNITS = 1.5f
private const val FOOTER_UNITS = 4f
private const val PHOTO_CORNER_UNITS = 0.4f
private const val CHIP_HEIGHT_UNITS = 2f
private const val CHIP_PADDING_UNITS = 0.8f
private const val LABEL_TEXT_UNITS = 0.8f
private const val BRAND_TEXT_UNITS = 0.9f
private const val GAP_UNITS = 1f

/** Rayon et coin coupé du repère, rapportés à sa hauteur (12 et 10 dp sur 32 dp dans l'application). */
private const val CHIP_CORNER_RATIO = 12f / 32f
private const val CHIP_CUT_RATIO = 10f / 32f

/** Graisse de la mention, semi-grasse. */
private const val BRAND_WEIGHT = 600
