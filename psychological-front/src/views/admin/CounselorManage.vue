<template>
  <div style="padding: 20px;">
    <!-- 工具栏：角色筛选 + 搜索 + 新增 -->
    <el-card style="margin-bottom: 12px;">
      <el-row :gutter="12" align="middle">
        <el-col :span="14">
          <!-- 角色 Tab 切换，对应功能说明中"咨询师/初访员/助理"三类 -->
          <el-radio-group v-model="filterRole" @change="getList">
            <el-radio-button value="">全部</el-radio-button>
            <el-radio-button value="counselor">咨询师</el-radio-button>
            <el-radio-button value="visitor">初访员</el-radio-button>
            <el-radio-button value="assistant">心理助理</el-radio-button>
          </el-radio-group>
        </el-col>
        <el-col :span="6">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索姓名/账号"
            clearable
            @input="handleSearch"
            @clear="getList"
          />
        </el-col>
        <el-col :span="4" style="text-align: right;">
          <el-button type="primary" @click="openAdd">+ 新增人员</el-button>
        </el-col>
      </el-row>
    </el-card>

    <el-card>
      <el-table :data="filteredList" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="账号（工号）" width="120" />
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="gender" label="性别" width="70" />
        <el-table-column prop="phone" label="电话" />
        <el-table-column prop="department" label="所属院系" />
        <el-table-column label="角色" width="100">
          <template #default="scope">
            <el-tag v-if="scope.row.role === 'counselor'" type="success">咨询师</el-tag>
            <el-tag v-else-if="scope.row.role === 'visitor'" type="primary">初访员</el-tag>
            <el-tag v-else-if="scope.row.role === 'assistant'" type="warning">心理助理</el-tag>
            <el-tag v-else type="info">{{ scope.row.role }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="scope">
            <el-button size="small" type="primary" plain @click="openEdit(scope.row)">编辑</el-button>
            <el-button size="small" type="danger" plain @click="del(scope.row.id, scope.row.name)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top: 12px; text-align: right; color: #909399; font-size: 13px;">
        共 {{ filteredList.length }} 条记录
      </div>
    </el-card>

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog v-model="show" :title="form.id ? '编辑人员信息' : '新增人员'" width="480px" :close-on-click-modal="false">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
        <el-form-item label="账号（工号）" prop="username">
          <el-input v-model="form.username" placeholder="请输入工号" :disabled="!!form.id" />
          <div v-if="!form.id" style="color:#999;font-size:12px;margin-top:4px;">
            初始密码为工号后6位，请告知本人登录后修改
          </div>
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="form.gender">
            <el-radio value="男">男</el-radio>
            <el-radio value="女">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="电话" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="所属院系" prop="department">
          <el-input v-model="form.department" placeholder="请输入院系" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" placeholder="请选择角色" style="width:100%">
            <el-option label="咨询师" value="counselor">
              <el-tag type="success" size="small">咨询师</el-tag>
              <span style="margin-left:8px;color:#666">负责正式咨询</span>
            </el-option>
            <el-option label="初访员" value="visitor">
              <el-tag type="primary" size="small">初访员</el-tag>
              <span style="margin-left:8px;color:#666">负责初次面谈</span>
            </el-option>
            <el-option label="心理助理" value="assistant">
              <el-tag type="warning" size="small">心理助理</el-tag>
              <span style="margin-left:8px;color:#666">负责安排协调</span>
            </el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="show = false">取消</el-button>
        <el-button type="primary" @click="save" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref([])
const form = ref({})
const show = ref(false)
const saving = ref(false)
const loading = ref(false)
const formRef = ref(null)
const filterRole = ref('')
const searchKeyword = ref('')

const rules = {
  username: [{ required: true, message: '工号不能为空', trigger: 'blur' }],
  name:     [{ required: true, message: '姓名不能为空', trigger: 'blur' }],
  role:     [{ required: true, message: '请选择角色', trigger: 'change' }],
}

// 前端搜索过滤（姓名或账号模糊匹配）
const filteredList = computed(() => {
  const kw = searchKeyword.value.trim().toLowerCase()
  if (!kw) return list.value
  return list.value.filter(u =>
    (u.name && u.name.toLowerCase().includes(kw)) ||
    (u.username && u.username.toLowerCase().includes(kw))
  )
})

const getList = async () => {
  loading.value = true
  try {
    const res = await request.get('/admin/user/list', {
      params: { role: filterRole.value || undefined }
    })
    // 只保留咨询相关角色（排除 student / admin）
    list.value = (res.data || []).filter(u =>
      ['counselor', 'visitor', 'assistant'].includes(u.role)
    )
  } catch (e) {
    ElMessage.error('获取列表失败')
    console.error(e)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  // 直接用 computed 过滤，不需要额外请求
}

const openAdd = () => {
  form.value = { role: 'counselor', gender: '女' }
  show.value = true
  formRef.value?.clearValidate()
}

const openEdit = (row) => {
  form.value = { ...row }
  show.value = true
  formRef.value?.clearValidate()
}

const save = async () => {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      await request.post('/admin/user/save', form.value)
      ElMessage.success(form.value.id ? '修改成功' : '新增成功，初始密码为工号后6位')
      show.value = false
      getList()
    } catch (e) {
      ElMessage.error('保存失败，请重试')
      console.error(e)
    } finally {
      saving.value = false
    }
  })
}

const del = async (id, name) => {
  try {
    await ElMessageBox.confirm(
      `确认删除「${name}」的账号？删除该用户将无法登录。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '确认删除', confirmButtonClass: 'el-button--danger' }
    )
    // 使用正确的 DELETE 方法
    await request.delete(`/admin/user/${id}`)
    ElMessage.success('删除成功')
    getList()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

onMounted(getList)
</script>
