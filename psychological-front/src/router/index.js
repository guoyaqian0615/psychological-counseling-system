import { createRouter, createWebHistory } from 'vue-router'
import Login from '@/views/Login.vue'
import { ElMessage } from 'element-plus'
const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', component: Login },
  // 管理员：Layout嵌套子菜单（原有不动）
  {
    path: '/admin',
    component: () => import('@/views/admin/Layout.vue'),
    redirect: '/admin/home',
    meta: { requiresAdmin: true },
    children: [
      { path: 'home', component: () => import('@/views/admin/AdminHome.vue') },
      { path: 'counselor', component: () => import('@/views/admin/CounselorManage.vue') },
      { path: 'duty', component: () => import('@/views/admin/DutyManage.vue') },
      { path: 'stat', component: () => import('@/views/admin/StatExport.vue') },
      { path: 'time-config', component: () => import('@/views/admin/TimeConfig.vue') },
      { path: 'visit-audit', component: () => import('@/views/admin/VisitAudit.vue') },
    ]
  },
  // 原有共用咨询师工作台（visitor/assistant/counselor共用/counsel）

  // 学生首页
  {
    path: '/student',
    component: () => import('@/views/student/StudentHome.vue'),
    meta: { requiresAuth: true, role: 'student' }
  },
  // 学生子页面（独立路由）
  { path: '/student/addVisit', component: () => import('@/views/student/AddVisit.vue') },
  { path: '/student/currentVisit', component: () => import('@/views/student/CurrentVisit.vue') },
  { path: '/questionResult', component: () => import('@/views/student/QuestionResult.vue') },
  // ==========【新增：原第二个路由的3个角色独立首页，统一权限管控】==========
  // 初访员首页
  {
    path: '/visitor/home',
    component: () => import('@/views/counsel/VisitorHome.vue'),
    meta: { requiresAuth: true, role: 'visitor' }
  },
  // 助理首页
  {
    path: '/assistant/home',
    component: () => import('@/views/counsel/AssistantHome.vue'),
    meta: { requiresAuth: true, role: 'assistant' }
  },
  // 咨询师首页
  {
    path: '/counselor/home',
    component: () => import('@/views/counsel/CounselorHome.vue'),
    meta: { requiresAuth: true, role: 'counselor' }
  },
  // 404兜底
  {
    path: '/:pathMatch(.*)*',
    redirect: '/login'
  }
]
const router = createRouter({
  history: createWebHistory(),
  routes
})

/**读取本地登录用户（原有逻辑完全保留不动） */
function getValidUser() {
  try {
    const userStr = localStorage.getItem('user')
    if (!userStr) return null
    const user = JSON.parse(userStr)
    if (!user || !user.id || !user.username || !user.role) {
      localStorage.removeItem('user')
      return null
    }
    return user
  } catch {
    localStorage.removeItem('user')
    return null
  }
}

// 全局路由守卫【原有逻辑无需改动，自动管控新增三个页面权限】
router.beforeEach((to) => {
  const user = getValidUser()
  // 管理员权限校验
  if (to.meta.requiresAdmin) {
    if (!user) {
      ElMessage.warning('请先登录')
      return '/login'
    }
    if (user.role !== 'admin') {
      ElMessage.error('无管理员权限')
      return '/login'
    }
    return true
  }
  // 角色页面权限校验
  if (to.meta.requiresAuth && to.meta.role) {
    if (!user) {
      ElMessage.warning('请先登录')
      return '/login'
    }
    const allowRole = Array.isArray(to.meta.role) ? to.meta.role : [to.meta.role]
    if (!allowRole.includes(user.role)) {
      ElMessage.error('无权访问当前页面')
      return '/login'
    }
    return true
  }

  // =========删掉了登录后访问login自动跳转首页的代码=========
  return true
})
export default router