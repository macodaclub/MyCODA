<script setup>
import videojs from "video.js";
import "videojs-youtube";
import {onBeforeUnmount, onMounted, ref} from "vue";

const videoPlayer = ref();
const videoPlayerEl = ref();

onMounted(() => {
  videoPlayer.value = videojs(videoPlayerEl.value, {
    controls: true,
    controlBar: {
      pictureInPictureToggle: false
    },
    autoplay: false,
    fluid: true,
    preload: 'auto',
    sources: [
      {
        src: 'https://www.youtube.com/watch?v=xa6bBXinvfc',
        type: 'video/youtube'
      }
    ],
  }, () => {
    console.log(videoPlayer.value);
  });
});
onBeforeUnmount(() => {
  videoPlayer.value.dispose();
});
</script>

<template>
  <div>
    <video ref="videoPlayerEl" class="video-js"></video>
  </div>
</template>

<style>
@import 'video.js/dist/video-js.css';
</style>