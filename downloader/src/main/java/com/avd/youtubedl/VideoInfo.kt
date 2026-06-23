package com.avd.youtubedl

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class VideoInfo {
    var id: String? = null
    var fulltitle: String? = null
    var title: String? = null

    //New fields
    @JsonProperty("is_live")
    var islive:Boolean? = null
    //End new fields

    @JsonProperty("upload_date")
    var uploadDate: String? = null

    @JsonProperty("display_id")
    var displayId: String? = null
    var duration = 0
    var tbr = 0
    var description: String? = null
    var thumbnail: String? = null
    var license: String? = null
    var extractor: String? = null

    @JsonProperty("extractor_key")
    var extractorKey: String? = null

    @JsonProperty("view_count")
    var viewCount: String? = null

    @JsonProperty("like_count")
    var likeCount: String? = null

    @JsonProperty("dislike_count")
    var dislikeCount: String? = null

    @JsonProperty("repost_count")
    var repostCount: String? = null

    @JsonProperty("average_rating")
    var averageRating: String? = null

    @JsonProperty("uploader_id")
    var uploaderId: String? = null
    var uploader: String? = null

    @JsonProperty("player_url")
    var playerUrl: String? = null

    @JsonProperty("webpage_url")
    var webpageUrl: String? = null

    @JsonProperty("webpage_url_basename")
    var webpageUrlBasename: String? = null
    var resolution: String? = null
    var width = 0
    var height = 0
    var format: String? = null

    @JsonProperty("format_id")
    var formatId: String? = null
    var ext: String? = null

    @JsonProperty("filesize")
    var fileSize: Long = 0

    @JsonProperty("filesize_approx")
    var fileSizeApproximate: Long = 0

    @JsonProperty("http_headers")
    var httpHeaders: Map<String, String>? = null
    var categories: ArrayList<String>? = null
    var tags: ArrayList<String>? = null

    @JsonProperty("requested_formats")
    var requestedFormats: ArrayList<VideoFormat>? = null
    var formats: ArrayList<VideoFormat>? = null
    var thumbnails: ArrayList<VideoThumbnail>? = null

    //private ArrayList<VideoSubtitle> subtitles;
    @JsonProperty("manifest_url")
    var manifestUrl: String? = null
    var url: String? = null
}