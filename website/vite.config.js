import {fileURLToPath, URL} from 'node:url'

import {defineConfig} from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
    plugins: [
        vue(),
    ],
    resolve: {
        alias: {
            '@': fileURLToPath(new URL('./src', import.meta.url))
        },
    },
    define: {
        'import.meta.env.MYCODA_ONTOLOGY_IRI_PREFIX': JSON.stringify(process.env.MYCODA_ONTOLOGY_IRI_PREFIX),
        'import.meta.env.GITHUB_REPO_URL': JSON.stringify(process.env.GITHUB_REPO_URL),
        'import.meta.env.MYCODA_ONTOLOGY_URL': JSON.stringify(process.env.MYCODA_ONTOLOGY_URL),
    }
})
