<template>
  <div class="page">
    <h2>我的当前心理咨询预约</h2>
    <el-table :data="tableData" :key="tableKey" border width="100%">
      <el-table-column label="序号" type="index" />
      <el-table-column label="初访员姓名" prop="visitorName"/>
      <el-table-column label="预约时间" prop="applyTime"/>
      <el-table-column label="预约状态" prop="status"/>
      <el-table-column label="预约地点" prop="location"/>
      <el-table-column label="操作">
        <template #default="scope">
          <el-button type="primary" @click="goResult(scope.row.id)">查看测评报告</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="reportVisible" title="心理健康测评报告" width="600px" append-to-body>
      <div style="padding: 10px; line-height: 1.8; font-size: 14px">
        {{ reportData.analysis }}
      </div>
      <div style="margin-top: 15px; font-weight: bold; color: #333">
        测评总分：{{ reportData.score }} 分｜测评状态：{{ reportData.level }}
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
const router = useRouter()
const tableData = ref([])
const tableKey = ref(0)

const reportVisible = ref(false)
const reportData = ref({})

const userInfo = JSON.parse(localStorage.getItem('user'))
const studentId = userInfo.userId

const loadData = async()=>{
  let res = await request.get('/student/current',{params:{studentId}})
  let arr = Array.isArray(res.data) ? res.data : (res.data ? [res.data] : [])
  tableData.value = []
  tableKey.value++
  tableData.value = [...arr]
}

const goResult = async (vid) => {
  try {
    let res = await request.get("/student/getQuestion/" + vid)
    reportData.value = res.data
    reportVisible.value = true
  } catch (e) {
    alert("获取测评报告失败")
  }
}

const cancelAppoint = (id)=>{
  if(!confirm('确定撤销本次预约？')) return
  request.post('/student/cancelVisit?id='+id).then(()=>{
    loadData()
  })
}
onMounted(loadData)
</script>

<style scoped>
.page{padding:20px}
</style>