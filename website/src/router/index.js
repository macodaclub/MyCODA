import {createRouter, createWebHistory} from "vue-router";
import Tools from "@/views/Tools.vue";
import MarkdownPage from "@/views/MarkdownPage.vue";
import homeMd from "@/assets/markdown/home.md?raw";
import educationMd from "@/assets/markdown/education.md?raw";
import eventsMd from "@/assets/markdown/events.md?raw";

export default createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
        {
            path: '/',
            name: 'home',
            component: MarkdownPage,
            props: {md: homeMd}
        },
        {
            path: '/education',
            name: 'education',
            component: MarkdownPage,
            props: {md: educationMd}
        },
        {
            path: '/events',
            name: 'events',
            component: MarkdownPage,
            props: {md: eventsMd}
        },
        {
            path: '/tools',
            name: 'tools',
            component: Tools
        }
    ]
})