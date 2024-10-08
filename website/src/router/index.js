import {createRouter, createWebHistory} from "vue-router";
import Browse from "@/views/Browse.vue";
import MarkdownPage from "@/views/MarkdownPage.vue";
import homeMd from "@/assets/markdown/home.md?raw";
import aboutMd from "@/assets/markdown/about.md?raw";
import SubmissionForm from "@/views/ArticleSubmission.vue";
import Contribute from "@/views/Contribute.vue";
import Curator from "@/views/Curator.vue";

export default createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
        {
            path: '/',
            name: 'home',
            component: MarkdownPage,
            props: {md: homeMd},
            meta: {
                title: 'Home'
            }
        },
        {
            path: '/about',
            name: 'about',
            component: MarkdownPage,
            props: {md: aboutMd},
            meta: {
                title: 'About'
            }
        },
        {
            path: '/browse',
            name: 'browse',
            component: Browse,
            meta: {
                title: 'Browse'
            }
        },
        {
            path: '/contribute',
            children: [
                {
                    path: '',
                    name: 'contribute',
                    component: Contribute,
                    meta: {
                        title: 'Contribute'
                    }
                },
                {
                    path: 'submit-paper',
                    name: 'submissionForm',
                    component: SubmissionForm,
                    meta: {
                        title: 'Article Submission'
                    }
                }
            ]
        },
        {
            path: '/curator',
            name: 'curator',
            component: Curator,
            meta: {
                title: 'Curator'
            }
        }
    ]
})