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
        'import.meta.env.MYCODA_ONTOLOGY_IRI_PREFIX': JSON.stringify("https://mycoda.ddns.net/ontologies/MYCODA"),
        'import.meta.env.GITHUB_REPO_URL': JSON.stringify("https://github.com/macodaclub/MyCODA"),
        'import.meta.env.MYCODA_ONTOLOGY_URL': JSON.stringify("https://raw.githubusercontent.com/macodaclub/MyCODA/refs/heads/main/ontologies/MaCODA.owl"),
    }
})
