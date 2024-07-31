import {createRouter, createWebHistory} from "vue-router";
import Browse from "@/views/Browse.vue";
import MarkdownPage from "@/views/MarkdownPage.vue";
import homeMd from "@/assets/markdown/home.md?raw";
import educationMd from "@/assets/markdown/education.md?raw";
import eventsMd from "@/assets/markdown/events.md?raw";
import SubmissionForm from "@/views/ArticleSubmission.vue";

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
            path: '/browse',
            name: 'browse',
            component: Browse
        },
        {
            path: '/submission-form',
            name: 'submissionForm',
            component: SubmissionForm
        }
    ]
})