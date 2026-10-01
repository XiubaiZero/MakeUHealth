
interface ImportMetaEnv {
    VITE_API_BASE_URL: string
    VITE_ASSISTANT_MODE: string
    VITE_ASSISTANT_API_URL: string
}

interface ImportMeta {
    readonly env: ImportMetaEnv
}