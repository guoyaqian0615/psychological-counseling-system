<template>
  <div class="p-4">
    <el-card>
      <h3>基本时间配置</h3>
      <el-form :model="form" label-width="200px" style="margin-top:20px">
        <el-form-item label="单次咨询时长（分钟）">
          <el-input-number v-model="form.singleDuration" :min="10" />
        </el-form-item>
        <el-form-item label="咨询间隔时间（分钟）">
          <el-input-number v-model="form.intervalMinute" :min="0" />
        </el-form-item>
        <el-form-item label="每日排班开始小时">
          <el-input-number v-model="form.dailyStartHour" :min="0" :max="23" />
        </el-form-item>
        <el-form-item label="每日排班结束小时">
          <el-input-number v-model="form.dailyEndHour" :min="1" :max="24" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="save">保存配置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const form = ref({
  singleDuration: 10,
  intervalMinute: 0,
  dailyStartHour: 8,
  dailyEndHour: 18
})

// 获取配置（加固，防止 undefined）
const get = async () => {
  try {
    const res = await request.get('/admin/timeConfig/get')
    // 只有 res 存在且有 data 才赋值
    if (res && res.data) {
      form.value = res.data
    }
  } catch (e) {
    ElMessage.error('获取时间配置失败')
    console.error('get error:', e)
  }
}

// 保存（重点：第49行附近已完全防护）
const save = async () => {
  // 简单校验
  if (!form.value.singleDuration || form.value.singleDuration < 10) {
    ElMessage.warning('请输入有效的单次咨询时长（至少10分钟）')
    return
  }
  if (form.value.dailyEndHour <= form.value.dailyStartHour) {
    ElMessage.warning('结束小时必须大于开始小时')
    return
  }

  try {
    const res = await request.post('/admin/timeConfig/save', form.value)
    console.log('save res:', res)
    // 后端code=200代表成功
    if (res?.code === 200) {
      ElMessage.success('保存成功') // 绿色弹窗
      get()
    } else {
      ElMessage.error(res?.msg || '保存失败')
    }
  } catch (e) {
    ElMessage.error('接口请求异常')
    console.error('save error:', e)
  }
}

onMounted(get)
</script>