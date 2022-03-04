
# Video Trimmer
[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg?style=flat-square)](https://www.apache.org/licenses/LICENSE-2.0.html)  
[![](https://jitpack.io/v/tinybeanskids/VideoTrimmer.svg)](https://jitpack.io/#tinybeanskids/VideoTrimmer)

<table>  
    <tr><td align="center"><img src="https://user-images.githubusercontent.com/3142641/88227598-22c49980-cc6e-11ea-85dc-454c5241901e.png" alt="Video Trimmer" width="50%"></td>  
    <tr><td align="center"><b>Video Trimmer</b></td>  
</table>  

## About Library
FFmpeg is a powerful multimedia framework which allows us to decode, encode, transcode, stream, filter and play most of the media content available now. With the help of these tools, you can develop and application that can manipulate any form of media to the desired output. Sky is not the limit when using FFmpeg. I prefer FFmpeg-all which is a Bible for FFmpeg but it is difficult to read if you do not know what you are looking for. To make it easy, I will summarise the basics of video manipulations using FFmpeg which, then you can use in your own applications or libraries that you are working on. I have developed a simple library that enables you to trim and crop a video, additionally you can compress any video and convert it into any format that you desire.  
Video encoding and decoding is done trough ffmpeg commands.

Resources used to write and read ffmpeg commands: https://ffmpeg.org/

Example of one ffmpeg command that just get file and output the same video.

```
ffmpeg -i input.mp4 output.avi 
```

<table>
  <tr>
    <th>Command</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>ffmpeg</td>
    <td>notify command line that you are trying to use ffmpeg</td>
  </tr>
  <tr>
    <td>—i input Input.mp4</td>
    <td>input video path/name</td>
  </tr>
  <tr>
    <td>Output.mp4</td>
    <td>output video path/name  </td>
  </tr>
</table>

Problems that Video Trimmer library solves:  
-loading and trimming normal speed (30fps) video and trimming it.  
-loading and trimming slow motion speed (120+fps) video and trimming it.

### What is slow motion video

<table>  
    <tr><td align="center"><img src="https://user-images.githubusercontent.com/29480276/156573732-2b7a15a5-a650-4352-bed3-5e6cc23a2ae1.png" alt="Video Trimmer" width="100%"></td>  
    <tr><td align="center"><b>How slow motion insert more frames to get slow motion effect</b></td>  
</table>  

Using ffmpeg to load and trim slow motion video brought many issues and one of them was using Exoplayer used to load video did not recognize slow motion video as video with 120 fps rather than video with 30fps in 4 times smaller speed.

Speeding or slowing down video using ffmpeg is time-consuming process.

The speed of a video stream can be changed by changing the presentation timestamp (PTS) of each video frame. This can be done via two methods: using the ​setpts video filter (which requires re-encoding) or by erasing the timestamps by exporting the video to a raw bitstream format and muxing to a container while creating new timestamps.

Function used to get slowmotion video in 30FPS in Video Trimmer Library

Using a complex filtergraph, you can speed up video and audio at the same time:

``` 
 ffmpeg -y -i input.mp4 -filter_complex "[0:v]setpts=3.3*PTS[v];[0:a]atempo=0.55,atempo=0.6,asetrate=44100*1.25,aformat=sample_rates=44100[a]" -async 1 -map "[v]" -async 1 -map "[a]" -async 1 -r 100 output.mp4 
```

  <table>
  <tr>
    <th>Command</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>ffmpeg</td>
    <td>notify command line that you are trying to use ffmpeg</td>
  </tr>
  <tr>
    <td>-y</td>
    <td>override existing output with new output.</td>
  </tr>
  <tr>
    <td>—i input Input.mp4</td>
    <td>input video path/name</td>
  </tr>
  <tr>
    <td>-filter_complex </td>
    <td>run complex ffmpeg command to manipulate with video and audio</td>
  </tr>
  <tr>
    <td>[0:v]setpts=3.3*PTS[v]</td>
    <td>slow PTS of video 3.3 times to match size of actual slow motion video </td>
  </tr>
  <tr>
    <td>[0:a]atempo=0.55,atempo=0.6,asetrate=44100*1.25,aformat=sample_rates=44100[a]</td>
    <td> slow PTS of audio 0.33 times to match size of actual slow motion audio</td>
  </tr>
  <tr>
    <td>-async 1 -map "[v]"</td>
    <td> asynchronously map video to output </td>
  </tr>
  <tr>
    <td> -async 1 -map "[a]"</td>
    <td> asynchronously map audio to output</td>
  </tr>
  <tr>
    <td>-async 1 -r 100</td>
    <td> asynchronously map framerate to 30 to output</td>
  </tr>
  <tr>
    <td>Output.mp4</td>
    <td>output video path/name  </td>
  </tr>
</table>

3.3 (video slow down) and 0.33 (audio slow down) have to be equal to 1 when multiplied to be slowed the same time, so video matches the audio of the video.

If loaded video is at 30fps it is loaded normally.

Ffmpeg function used to trim video

```
ffmpeg -y  -i input.mp4 -ss 00:05:20 -t 00:10:00 -c:v copy -c:a copy output.mp4  
```

 <table>
  <tr>
    <th>Command</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>ffmpeg</td>
    <td>notify command line that you are trying to use ffmpeg</td>
  </tr>
  <tr>
    <td>-y</td>
    <td>override existing output with new output</td>
  </tr>
  <tr>
    <td>—i input Input.mp4</td>
    <td>input video path/name</td>
  </tr>
  <tr>
    <td>-ss 00:05:20</td>
    <td>ffmpeg command to set set video trimm start</td>
  </tr>
  <tr>
    <td>-t 00:10:00</td>
    <td>ffmpeg command to set set video trimm end</td>
  </tr>
  <tr>
    <td>-c:v copy</td>
    <td>copy video codec from input file output</td>
  </tr>
  <tr>
    <td>-c:a copy</td>
    <td>copy audio codec from input file output</td>
  </tr>
  <tr>
    <td>Output.mp4</td>
    <td>output video path/name  </td>
  </tr>
</table>

## Implementation
### [1] In your app module gradle file
```gradle  
dependencies {  
 implementation 'com.github.tinybeanskids:videoeditor:1.0.5'}  
```  

### [2] In your project level gradle file
```gradle  
allprojects {  
 repositories { maven { url 'https://jitpack.io' } }}  
```  
### [3] Use VideoTrimmer in your layout.xml
```xml  
<com.video.trimmer.view.VideoTrimmer  
  android:id="@+id/videoTrimmer"  
  android:layout_width="match_parent"  
  android:layout_height="match_parent"  
  android:layout_below="@+id/header"/>  
```  
### [4] Implement OnTrimVideoListener on your Activity/Fragment
```kotlin  
class MainActivity : AppCompatActivity(), OnTrimVideoListener {  
 ... 
 override fun getResult(uri: Uri){ } 
 override fun cancelAction(){ } 
 override fun onError(message: String){ } 
 override fun onInfo(info: String){ }}  
```  
### [5] Implement OnVideoListener on your Activity/Fragment
```kotlin  
class MainActivity : AppCompatActivity(), OnTrimVideoListener {  
 ... override fun onFFmpegStarted(uri: Uri){ } 
 override fun onFFmpegError(){ } 
 override fun onFFmpegFinished(message: String){ }}  
```  

### [6] Create instances and set default values for the VideoTrimmer in your Activity/ Fragment
```kotlin  
videoTrimmer.setTextTimeSelectionTypeface(FontsHelper[this, FontsConstants.SEMI_BOLD])  
 .setOnTrimVideoListener(this) 
 .setOnVideoListener(this) 
 .encodeSlowMotion(uri) 
 .setVideoInformationVisibility(true) 
 .setDestinationFile(file)
 ```
