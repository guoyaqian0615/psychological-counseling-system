<template>
  <div class="page">
    <el-card>
      <h3>心理问卷测评结果</h3>
      <el-divider/>
      <p>测评总分：<span style="font-size:22px;color:#409EFF">{{score}}</span></p>
      <el-divider/>
      <p style="font-size:16px;">测评分析：{{analysis}}</p>
      <el-button style="margin-top:20px" @click="$router.back()">返回</el-button>
    </el-card>
  </div>
</template>

<script setup>
import {ref,onMounted} from 'vue'
import {useRoute} from 'vue-router'
import axios from 'axios'
const route = useRoute()
const score = ref(0)
const analysis = ref('')
const visitId = route.query.visitId

const getResult = async()=>{
  let res = await axios.get(`/api/student/getQuestion/${visitId}`)
  if(res.data.code===200){
    score.value = res.data.data.score
    analysis.value = res.data.data.analysis
  }
}
onMounted(getResult)
</script>
<style scoped>
.page{padding:30px}
</style>