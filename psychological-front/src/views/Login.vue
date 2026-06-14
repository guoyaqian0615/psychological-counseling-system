<template>
  <div class="login-bg">
    <div class="login-card">
      <div class="left-panel">
        <h1 class="brand-title">心理咨询<br/>管理系统</h1>
        <p class="brand-sub">用心守护·健康同行</p>
        <ul class="brand-features">
          <li><span class="dot" />预约管理</li>
          <li><span class="dot" />咨询记录</li>
          <li><span class="dot" />数据统计</li>
          <li><span class="dot" />结案归档</li>
        </ul>
      </div>

      <div class="right-panel">
        <h2 class="form-title">欢迎登录</h2>

        <div class="role-tabs">
          <button
            v-for="tab in tabs" :key="tab.key"
            class="role-btn"
            :class="{ active: activeTab === tab.key }"
            @click="switchTab(tab.key)"
          >
            <span class="tab-icon">{{ tab.icon }}</span>
            {{ tab.label }}
          </button>
        </div>

        <el-form :model="loginForm" :rules="loginRules" ref="loginRef" @submit.prevent>
          <el-form-item prop="username">
            <el-input
              v-model="loginForm.username"
              :placeholder="activeTab === 'student' ? '请输入学号' : '请输入工号'"
              size="large"
              clearable
            >
              <template #prefix><span style="font-size:15px">👤</span></template>
            </el-input>
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              show-password
            >
              <template #prefix><span style="font-size:15px">🔒</span></template>
            </el-input>
          </el-form-item>

          <el-form-item prop="role" v-if="activeTab === 'counsel'">
            <el-select
              v-model="loginForm.role"
              placeholder="请选择您的身份"
              size="large"
              style="width:100%"
            >
              <el-option label="🚪 初访员"  value="visitor" />
              <el-option label="🤝 心理助理" value="assistant" />
              <el-option label="💼 咨询师"  value="counselor" />
            </el-select>
          </el-form-item>

          <el-form-item>
            <el-button
              class="login-btn"
              type="primary"
              :loading="loginLoading"
              size="large"
              @click="handleLogin"
            >
              {{ loginLoading ? '登录中…' : '登 录' }}
            </el-button>
          </el-form-item>
        </el-form>

        <div v-if="activeTab === 'student'" class="register-link">
          <span>还没有账号？</span>
          <el-link type="primary" :underline="false" @click="openRegister">立即注册</el-link>
        </div>
      </div>
    </div>

    <el-dialog
      v-model="showRegister"
      title="学生注册"
      width="480px"
      :close-on-click-modal="false"
      class="register-dialog"
      align-center
    >
      <el-form
        :model="regForm"
        :rules="regRules"
        ref="regRef"
        label-width="85px"
        style="padding: 0 8px"
      >
        <el-form-item label="学号" prop="username">
          <el-input v-model="regForm.username" placeholder="请输入学号（账号）" clearable />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="regForm.name" placeholder="请输入真实姓名" clearable />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="regForm.gender">
            <el-radio value="男">男</el-radio>
            <el-radio value="女">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="院系" prop="department">
          <el-input v-model="regForm.department" placeholder="请输入所在院系" clearable />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="regForm.phone" placeholder="请输入手机号" clearable />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="regForm.password" type="password" placeholder="请设置登录密码（6位以上）" show-password clearable />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="regForm.confirmPassword" type="password" placeholder="请再次输入密码" show-password clearable />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRegister = false">取消</el-button>
        <el-button type="primary" :loading="regLoading" @click="handleRegister">注册</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import { useRouter } from 'vue-router'

const router = useRouter()
const BASE = 'http://localhost:8080'

const tabs = [
  { key: 'student', label: '学生端',     icon: '🎓' },
  { key: 'counsel', label: '咨询人员端', icon: '🏥' },
  { key: 'admin',   label: '管理员端',   icon: '⚙️' },
]

const activeTab = ref('student')

const switchTab = (key) => {
  activeTab.value = key
  loginForm.username = ''
  loginForm.password = ''
  loginForm.role     = ''
  loginRef.value?.clearValidate()
}

const loginRef  = ref()
const loginLoading = ref(false)
const loginForm = reactive({ username: '', password: '', role: '' })

const loginRules = computed(() => ({
  username: [{ required: true, message: '账号不能为空', trigger: 'blur' }],
  password: [{ required: true, message: '密码不能为空', trigger: 'blur' }],
  ...(activeTab.value === 'counsel'
    ? { role: [{ required: true, message: '请选择身份', trigger: 'change' }] }
    : {})
}))

const TAB_VALID_ROLES = {
  student: ['student'],
  counsel: ['visitor', 'assistant', 'counselor'],
  admin:   ['admin'],
}

const TAB_URL = {
  student: `${BASE}/student/login`,
  counsel: `${BASE}/counsel/login`,
  admin:   `${BASE}/admin/login`,
}

const ROLE_ROUTE = {
  student:   '/student',
  visitor:   '/visitor/home',
  assistant: '/assistant/home',
  counselor: '/counselor/home',
  admin:     '/admin/home',
}

const handleLogin = async () => {
  const valid = await loginRef.value.validate().catch(() => false);
  if (!valid) return;

  loginLoading.value = true;
  try {
    const res = await axios.post(TAB_URL[activeTab.value], {
      username: loginForm.username,
      password: loginForm.password,
      role: loginForm.role || undefined,
    });

    if (res.data.code === 200) {
      const user = res.data.data;
      const validRoles = TAB_VALID_ROLES[activeTab.value];

      if (!user.role || !validRoles.includes(user.role)) {
        ElMessage.error("身份验证失败，请在对应入口登录");
        return;
      }

      localStorage.setItem(
        "user",
        JSON.stringify({
          id: user.id,
          userId: user.id,
          username: user.username,
          name: user.name,
          role: user.role,
        })
      );
      localStorage.setItem("stuId", user.id);
      ElMessage.success("登录成功");
      router.push(ROLE_ROUTE[user.role] || "/login");
    } else {
      ElMessage.error(res.data.msg || "登录失败，请检查账号密码");
    }
  } catch {
    ElMessage.error("服务器异常，请稍后重试");
  } finally {
    loginLoading.value = false;
  }
};

const showRegister = ref(false)
const regRef       = ref()
const regLoading   = ref(false)

const regForm = reactive({
  username: '', name: '', gender: '男', department: '',
  phone: '', password: '', confirmPassword: '',
})

const validateConfirmPwd = (_, value, callback) => {
  if (value !== regForm.password) callback(new Error('两次密码不一致'))
  else callback()
}

const regRules = {
  username: [{ required: true, message: '学号不能为空', trigger: 'blur' }],
  name: [{ required: true, message: '姓名不能为空', trigger: 'blur' }],
  department: [{ required: true, message: '请输入院系', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '密码不能为空', trigger: 'blur' },
    { min: 6, message: '密码不少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPwd, trigger: 'blur' }
  ]
}

const openRegister = () => {
  Object.assign(regForm, {
    username: '', name: '', gender: '男', department: '',
    phone: '', password: '', confirmPassword: '',
  })
  regRef.value?.clearValidate()
  showRegister.value = true
}

const handleRegister = async () => {
  const valid = await regRef.value.validate().catch(() => false)
  if (!valid) return

  regLoading.value = true
  try {
    const res = await axios.post(`${BASE}/student/register`, {
      username:   regForm.username,
      password:   regForm.password,
      name:       regForm.name,
      gender:     regForm.gender,
      department: regForm.department,
      phone:      regForm.phone,
      role:       'student',
    })

    if (res.data.code === 200) {
      ElMessage.success('注册成功，请登录')
      showRegister.value = false
      loginForm.username = regForm.username
    } else {
      ElMessage.error(res.data.msg || '注册失败')
    }
  } catch {
    ElMessage.error('服务器异常，请稍后重试')
  } finally {
    regLoading.value = false
  }
}
</script>

<style scoped>
.login-bg {
  min-height: 100vh;
  background: url('https://guoyaqian.oss-cn-beijing.aliyuncs.com/90f5b374a5b67155e7f62e8712c6c20c.jpg') center/cover no-repeat;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  font-family: 'Noto Sans SC', 'PingFang SC', sans-serif;
}

.login-card {
  width: 860px;
  min-height: 500px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  border-radius: 20px;
  box-shadow: 0 10px 50px rgba(0, 0, 0, 0.08);
  display: flex;
  overflow: hidden;
  position: relative;
  z-index: 1;
}

.left-panel {
  width: 310px;
  flex-shrink: 0;
  background: rgba(116, 194, 150, 0.15);
  padding: 50px 32px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  color: #2A3B3C;
}

.brand-title {
  font-size: 26px;
  font-weight: 600;
  line-height: 1.4;
  margin-bottom: 8px;
}
.brand-sub {
  font-size: 13px;
  color: #556667;
  margin-bottom: 30px;
}
.brand-features {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.brand-features li {
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 10px;
  color: #445556;
}
.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #6a9f82;
  flex-shrink: 0;
}

.right-panel {
  flex: 1;
  padding: 50px 42px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.form-title {
  font-size: 22px;
  font-weight: 600;
  color: #2d3738;
  margin-bottom: 26px;
}

.role-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 28px;
}
.role-btn {
  flex: 1;
  padding: 10px 6px;
  font-size: 13px;
  border: 1px solid #e0e6e6;
  border-radius: 10px;
  background: rgba(255,255,255,0.6);
  color: #778889;
  cursor: pointer;
  transition: all 0.25s ease;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}
.role-btn .tab-icon { font-size: 18px; }
.role-btn:hover {
  border-color: #c0d8cc;
  color: #557766;
}
.role-btn.active {
  background: rgba(116, 194, 150, 0.12);
  border-color: #9fc3ae;
  color: #4a6b5a;
  font-weight: 500;
}

:deep(.el-input__wrapper) {
  background: rgba(255,255,255,0.7) !important;
  border: 1px solid #e0e6e6 !important;
  box-shadow: none !important;
  border-radius: 10px !important;
}
:deep(.el-input__wrapper:hover),
:deep(.el-input__wrapper.is-focus) {
  border-color: #9fc3ae !important;
}
:deep(.el-input__inner) {
  color: #334444 !important;
}
:deep(.el-input__inner::placeholder) {
  color: #a0b0b0 !important;
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  letter-spacing: 2px;
  border-radius: 10px !important;
  background: #6a9f82 !important;
  border: none !important;
  opacity: 0.92;
}
.login-btn:hover {
  opacity: 1;
}

.register-link {
  text-align: center;
  font-size: 13px;
  color: #778889;
  margin-top: 6px;
}
:deep(.el-link.el-link--primary) {
  color: #6a9f82 !important;
}

:deep(.register-dialog .el-dialog) {
  background: #ffffff !important;
  border-radius: 16px !important;
}
</style>