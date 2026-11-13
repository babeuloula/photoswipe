package fr.babeuloula.photoswipe.domain.fixtures

import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.MotionPhoto
import fr.babeuloula.photoswipe.domain.model.MotionPhotoFormat
import fr.babeuloula.photoswipe.domain.port.MotionPhotoReader
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** [MotionPhotoReader] qui rend les photos animées de [motionPhotos] et compte ses lectures. */
class FakeMotionPhotoReader(
    private val motionPhotos: Map<MediaId, MotionPhoto> = emptyMap(),
) : MotionPhotoReader {
    /** Médias lus, dans l'ordre, pour les vérifications du test. */
    val read = mutableListOf<MediaId>()

    override suspend fun motionPhoto(media: MediaItem): MotionPhoto? {
        read += media.id
        return motionPhotos[media.id]
    }
}

/** Photo animée de Google quelconque, pour les tests qui n'en lisent pas le fichier. */
fun aMotionPhoto(
    videoStart: Long = 1_000,
    videoLength: Long = 500,
    format: MotionPhotoFormat = MotionPhotoFormat.GOOGLE_XMP,
): MotionPhoto = MotionPhoto(format, videoStart, videoLength)

/** Début d'un MP4 : une boîte `ftyp`, puis [extra] octets quelconques. */
fun aMp4(extra: Int = 32): ByteArray =
    byteArrayOf(0, 0, 0, 0x18) + "ftypmp42".toByteArray(Charsets.US_ASCII) + ByteArray(extra) { 0x11 }

/**
 * JPEG minimal (SOI, segment APP1 portant [xmp] s'il est donné, quelques octets d'image, EOI),
 * suivi de [trailer] : ce qu'un appareil met après la photo.
 */
fun aJpeg(
    xmp: String? = null,
    trailer: ByteArray = ByteArray(0),
): ByteArray {
    val out = ByteArrayOutputStream()
    out.write(byteArrayOf(0xFF.toByte(), 0xD8.toByte()))
    if (xmp != null) {
        val payload = "http://ns.adobe.com/xap/1.0/\u0000$xmp".toByteArray(Charsets.UTF_8)
        out.write(byteArrayOf(0xFF.toByte(), 0xE1.toByte()))
        out.write(ByteBuffer.allocate(2).putShort((payload.size + 2).toShort()).array())
        out.write(payload)
    }
    out.write(ByteArray(64) { 0x42 })
    out.write(byteArrayOf(0xFF.toByte(), 0xD9.toByte()))
    out.write(trailer)
    return out.toByteArray()
}

/** XMP d'une photo animée de Google au format « Micro Video » : la vidéo occupe les [offset] derniers octets. */
fun aMicroVideoXmp(
    offset: Int,
    flag: String = "1",
): String =
    """<x:xmpmeta xmlns:x="adobe:ns:meta/"><rdf:RDF><rdf:Description
        xmlns:GCamera="http://ns.google.com/photos/1.0/camera/"
        GCamera:MicroVideoVersion="1" GCamera:MicroVideo="$flag" GCamera:MicroVideoOffset="$offset"
        GCamera:MicroVideoPresentationTimestampUs="1500000"/></rdf:RDF></x:xmpmeta>"""

/** XMP d'une photo animée de Google au format « Motion Photo » : conteneur photo puis vidéo de [videoLength] octets. */
fun aMotionPhotoXmp(videoLength: Int): String =
    """<x:xmpmeta xmlns:x="adobe:ns:meta/"><rdf:RDF><rdf:Description
        xmlns:Camera="http://ns.google.com/photos/1.0/camera/"
        xmlns:Container="http://ns.google.com/photos/1.0/container/"
        xmlns:Item="http://ns.google.com/photos/1.0/container/item/"
        Camera:MotionPhoto="1" Camera:MotionPhotoVersion="1" Camera:MotionPhotoPresentationTimestampUs="0">
        <Container:Directory><rdf:Seq>
        <rdf:li rdf:parseType="Resource"><Container:Item Item:Mime="image/jpeg" Item:Semantic="Primary" Item:Length="0" Item:Padding="0"/></rdf:li>
        <rdf:li rdf:parseType="Resource"><Container:Item Item:Mime="video/mp4" Item:Semantic="MotionPhoto" Item:Length="$videoLength"/></rdf:li>
        </rdf:Seq></Container:Directory></rdf:Description></rdf:RDF></x:xmpmeta>"""

/**
 * Trailer de Samsung qui range [video] dans un bloc `MotionPhoto_Data`, suivi de son répertoire
 * `SEFH`, de la taille du répertoire et de `SEFT`.
 */
fun aSamsungTrailer(video: ByteArray): ByteArray {
    val name = "MotionPhoto_Data".toByteArray(Charsets.US_ASCII)
    val block = le(0x00, 0x00, 0x30, 0x0A) + le32(name.size) + name + video
    val directory =
        "SEFH".toByteArray(Charsets.US_ASCII) + le32(SEF_VERSION) + le32(1) +
            le(0x00, 0x00, 0x30, 0x0A) + le32(block.size) + le32(block.size)
    return block + directory + le32(directory.size) + "SEFT".toByteArray(Charsets.US_ASCII)
}

private fun le(vararg bytes: Int): ByteArray = ByteArray(bytes.size) { bytes[it].toByte() }

private fun le32(value: Int): ByteArray =
    ByteBuffer
        .allocate(4)
        .order(ByteOrder.LITTLE_ENDIAN)
        .putInt(value)
        .array()

/** Version du format SEF écrite par les appareils récents. */
private const val SEF_VERSION = 106
