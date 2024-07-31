<script setup>
import headerBgUrl from '@/assets/img/headerBg.png'
</script>

<template>
  <header :style="{backgroundImage: `linear-gradient(to right, rgba(24, 9, 2, 0.9), rgba(24, 9, 2, 0.5)), linear-gradient(to top, rgba(24, 9, 2, 0.8), rgba(24, 9, 2, 0.5)),  url(${headerBgUrl})`}">
    <div class="title-container-wrapper mb-2">
      <div class="title-container flex flex-col gap-1">
        <h1 class="title text-primary font-extrabold text-4xl mt-2">MYCODA</h1>
        <h4 class="heading text-surface-0 font-bold text-md">Many Criteria Optimization and Decision Analysis (MaCODA) Platform</h4>
      </div>
    </div>
    <div class="nav-bar">
      <router-link to="/" tag="div" class="nav-btn">Home</router-link>
      <router-link to="/education" tag="div" class="nav-btn">Education</router-link>
      <router-link to="/events" tag="div" class="nav-btn">Events</router-link>
      <router-link to="/browse" tag="div" class="nav-btn">Browse</router-link>
      <router-link to="/submission-form" tag="div" class="nav-btn">Contribute</router-link>
    </div>
  </header>
</template>

<style scoped>
header {

  background-color: #ffffff;
  background-size: cover;
  background-position: center;

  .title-container-wrapper {
    padding: 0 1rem;
  }

  .title-container {
    line-height: 1.5;
    max-width: 1280px;
    margin: 0 auto;
    padding: 1rem 0;

    .title {
      text-shadow: 2px 0 #00000080, -2px 0 #00000080, 0 2px #00000080, 0 -2px #00000080;
    }
  }

  .nav-bar {
    max-width: calc(1280px + 2rem);
    margin: 0 auto;

    @media (min-width: 1024px) {
      display: flex;
      flex-direction: row;
    }

    .nav-btn {
      display: block;
      font-weight: bold;
      transition: 0.4s;
      padding: 0.5rem 1rem;
    }

    .nav-btn {
      display: block;
      font-weight: bold;
      transition: 0.4s;
      padding: 0.5rem 1rem;
      color: #FFFFFF;
    }

    .nav-btn:hover {
      background-color: var(--color-background-mute);
      color: var(--p-primary-color);
    }
  }
}
.router-link-active {
  @apply bg-surface-50 rounded-t-lg;
  color: var(--p-primary-color) !important;
  font-weight: 800 !important;
}
</style>