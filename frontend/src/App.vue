<script setup>
import Header from "@/components/Header.vue";
</script>

<template>
    <Header/>
    <main>
        <div class="main-content">
            <router-view/>
        </div>
    </main>
</template>

<style scoped>
main {
    padding: 0 1rem;
}

main .main-content {
    max-width: 1280px;
    margin: 0 auto;
    padding: 1rem 0;
}
</style>