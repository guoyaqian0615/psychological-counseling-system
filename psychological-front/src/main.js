// 全局屏蔽 ResizeObserver 报错（彻底解决 Tab 切换弹黑框）
const debounce = (fn, delay) => {
    let timer = null
    return function () {
        clearTimeout(timer)
        timer = setTimeout(() => {
            fn.apply(this, arguments)
        }, delay)
    }
}
const _ResizeObserver = window.ResizeObserver
window.ResizeObserver = class ResizeObserver extends _ResizeObserver {
    constructor(callback) {
        super(debounce(callback, 20))
    }
}

// 基础 Vue 应用初始化
import { createApp } from 'vue'
import App from './App.vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import router from './router'

const app = createApp(App)
app.use(ElementPlus)
app.use(router)
app.mount('#app')