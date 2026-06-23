package com.avd.youtubedl

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class VideoFormat {
    var asr = 0
    var tbr = 0
    var abr = 0
    var format: String? = null

    @JsonProperty("format_id")
    var formatId: String? = null

    @JsonProperty("format_note")
    var formatNote: String? = null
    var ext: String? = null
    var preference = 0
    var vcodec: String? = null
    var acodec: String? = null
    var width = 0
    var height = 0

    @JsonProperty("filesize")
    var fileSize: Long = 0

    @JsonProperty("filesize_approx")
    var fileSizeApproximate: Long = 0
    var fps = 0
    var url: String? = null

    @JsonProperty("manifest_url")
    var manifestUrl: String? = null

    @JsonProperty("http_headers")
    var httpHeaders: Map<String, String>? = null
}