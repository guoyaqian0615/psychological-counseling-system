<template>
  <el-container style="height: 100vh; margin: 0; padding:0;">
    <!-- 左侧侧边栏 -->
    <el-aside width="230px" class="side-box">
      <!-- 顶部标题栏 -->
      <div class="side-title">心理咨询系统</div>

      <!-- 菜单区域 -->
      <el-menu
        router
        :default-active="$route.path"
        class="side-menu"
        background-color="#27374D"
        text-color="#C5D3E0"
        active-text-color="#fff"
      >
        <el-menu-item index="/admin/home">
          <el-icon><House /></el-icon>
          <span>首页</span>
        </el-menu-item>
        <el-menu-item index="/admin/counselor">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/time-config">
          <el-icon><Clock /></el-icon>
          <span>时间配置</span>
        </el-menu-item>
        <el-menu-item index="/admin/duty">
          <el-icon><Calendar /></el-icon>
          <span>值班管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/visit-audit">
          <el-icon><Document /></el-icon>
          <span>初访审核</span>
        </el-menu-item>
        <el-menu-item index="/admin/stat">
          <el-icon><DataAnalysis /></el-icon>
          <span>统计导出</span>
        </el-menu-item>
      </el-menu>

      <!-- 底部退出 -->
      <div class="logout-btn" @click="handleLogout">
        <el-icon><SwitchButton /></el-icon>
        <span>退出登录</span>
      </div>
    </el-aside>

    <!-- 右侧主体 -->
    <el-container>
      <el-header class="top-header">{{ pageName }}</el-header>
      <el-main class="main-body">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { House, User, Clock, Calendar, Document, DataAnalysis, SwitchButton } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

const titleMap = {
  '/admin/home': '首页',
  '/admin/counselor': '用户管理',
  '/admin/time-config': '时间配置',
  '/admin/duty': '值班管理',
  '/admin/visit-audit': '初访审核',
  '/admin/stat': '统计导出'
}
const pageName = computed(() => titleMap[route.path] || '管理首页')

// 退出登录
const handleLogout = () => {
  localStorage.removeItem('user')
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<style scoped>
.side-box {
  background-color: #27374D;
  height: 100%;
  display: flex;
  flex-direction: column;
}
/*顶部标题*/
.side-title {
  height:70px;
  line-height:70px;
  text-align:center;
  font-size:24px;
  color:#ffffff;
  font-weight:bold;
  background:#1F2E40;
}
/*菜单样式*/
.side-menu {
  flex:1;
  border:none;
  padding:15px 8px;
}
:deep(.el-menu-item) {
  height:48px;
  line-height:48px;
  border-radius:10px;
  margin-bottom:8px;
  padding-left:12px !important;
}
:deep(.el-menu-item.is-active) {
  background:#526D82 !important;
}
:deep(.el-menu-item:hover) {
  background:#3C516B !important;
}
/*底部退出*/
.logout-btn {
  height:52px;
  line-height:52px;
  text-align:center;
  color:#C5D3E0;
  border-top:1px solid #3C516B;
  cursor:pointer;
  transition:0.2s;
}
.logout-btn:hover {
  background:#3C516B;
  color:#fff;
}
/*顶部栏*/
.top-header {
  height:60px;
  line-height:60px;
  background:#fff;
  border-bottom:1px solid #e6e9ec;
  padding:0 25px;
  font-size:16px;
  font-weight:500;
}
/*内容区域*/
.main-body {
  background:#F4F7FA;
  padding:20px;
}
</style>

<style>
*{margin:0;padding:0;box-sizing:border-box;}
html,body{height:100%;}
</style>