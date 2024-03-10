<script setup>

</script>

<template>
    <header>
        <div class="title-container-wrapper">
            <div class="title-container">
                <h1 class="title">MYCODA</h1>
                <h4 class="heading">Many Criteria Optimization and Decision Analysis (MaCODA) Platform</h4>
            </div>
        </div>
        <div class="nav-bar">
            <router-link to="/" tag="div" class="nav-btn">Home</router-link>
            <router-link to="/education" tag="div" class="nav-btn">Education</router-link>
            <router-link to="/events" tag="div" class="nav-btn">Events</router-link>
            <router-link to="/resources" tag="div" class="nav-btn">Resources</router-link>
            <router-link to="/tools" tag="div" class="nav-btn">MyCODA Tools</router-link>
            <router-link to="/forum" tag="div" class="nav-btn">Forum</router-link>
            <router-link to="/faq" tag="div" class="nav-btn">FAQ</router-link>
            <router-link to="/contact" tag="div" class="nav-btn">Contact/Join Us</router-link>
            <router-link to="/about" tag="div" class="nav-btn">About Us</router-link>
        </div>
        <div class="border"/>
    </header>
</template>

<style scoped>
header .title-container-wrapper {
    padding: 0 1rem;
}

header .title-container {
    line-height: 1.5;
    max-width: 1280px;
    margin: 0 auto;
    padding: 1rem 0;
}

.border {
    border-bottom: 1px solid var(--color-border);
}

.title {
    color: var(--color-orange);
    font-weight: bold;
}

header .nav-bar {
    max-width: calc(1280px + 2rem);
    margin: 0 auto;
}

header .nav-bar .nav-btn {
    display: block;
    font-weight: bold;
    color: var(--color-heading);
    transition: 0.4s;
    padding: 0.5rem 1rem;
}

header .nav-bar .nav-btn:hover {
    background-color: var(--color-background-mute);
}

header .nav-bar .nav-btn.router-link-active {
    background-color: var(--color-background-mute);
    color: var(--color-orange)
}

@media (min-width: 1024px) {
    header .nav-bar {
        display: flex;
        flex-direction: row;
    }
}
</style>