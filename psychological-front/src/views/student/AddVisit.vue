<template>
  <div class="page-container">
    <!-- 1. 知情同意书弹窗 -->
    <el-dialog
      v-model="showAgreement"
      title="心理咨询知情同意书"
      width="750px"
      close-on-click-modal="false"
      close-on-press-escape="false"
      show-close="false"
    >
      <div class="agreement-box">
        <h3>心理咨询知情同意书</h3>
        <p>1. 本次咨询仅用于心理健康初访评估，不涉及临床诊断。</p>
        <p>2. 你填写的内容将严格保密，仅用于心理中心工作使用。</p>
        <p>3. 请如实填写问卷，以便咨询师为你提供更准确的帮助。</p>
        <p>4. 预约成功后请按时到场，如需取消请提前联系心理中心。</p>
        <p>5. 提交即代表你已阅读、理解并同意以上条款。</p>
      </div>

      <div style="margin: 20px 0 10px 10px">
        <el-checkbox v-model="isAgreed">
          我已认真阅读并同意以上内容
        </el-checkbox>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="goBack">取消返回</el-button>
          <el-button
            type="primary"
            @click="agreeToContinue"
            :disabled="!isAgreed"
          >
            确认并进入问卷
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 2. 心理问卷页面 -->
    <div v-if="step === 2" class="paper-box">
      <el-card shadow="hover">
        <div class="paper-title">心理健康调查问卷</div>
        <div class="paper-desc">请根据你最近两周的真实情况选择</div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-width="360px"
          hide-required-asterisk
          :show-message="false"
          class="paper-form"
        >
          <el-form-item label="1. 近两周整体睡眠状况" prop="q1">
            <el-radio-group v-model="form.q1">
              <el-radio label="2">睡眠充足，入睡顺畅</el-radio>
              <el-radio label="1">偶尔失眠，睡眠一般</el-radio>
              <el-radio label="0">经常失眠，睡眠较差</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="2. 近两周是否频繁情绪低落" prop="q2">
            <el-radio-group v-model="form.q2">
              <el-radio label="2">几乎没有</el-radio>
              <el-radio label="1">偶尔出现</el-radio>
              <el-radio label="0">经常出现</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="3. 近两周是否感到焦虑、紧张不安" prop="q3">
            <el-radio-group v-model="form.q3">
              <el-radio label="2">完全没有</el-radio>
              <el-radio label="1">轻微焦虑</el-radio>
              <el-radio label="0">明显焦虑</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="4. 对以往喜爱的事物是否仍有兴趣" prop="q4">
            <el-radio-group v-model="form.q4">
              <el-radio label="2">兴趣依旧浓厚</el-radio>
              <el-radio label="1">兴趣有所减退</el-radio>
              <el-radio label="0">几乎丧失兴趣</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="5. 近两周是否持续身体疲惫、乏力" prop="q5">
            <el-radio-group v-model="form.q5">
              <el-radio label="2">精力充沛</el-radio>
              <el-radio label="1">容易疲惫</el-radio>
              <el-radio label="0">整日疲惫不堪</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="6. 近两周食欲与体重有无明显变化" prop="q6">
            <el-radio-group v-model="form.q6">
              <el-radio label="2">无任何变化</el-radio>
              <el-radio label="1">轻微变化</el-radio>
              <el-radio label="0">变化非常明显</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="7. 是否存在人际交往困扰" prop="q7">
            <el-radio-group v-model="form.q7">
              <el-radio label="2">相处融洽，无困扰</el-radio>
              <el-radio label="1">偶尔存在小困扰</el-radio>
              <el-radio label="0">存在较大社交压力</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="8. 近两周是否有自我否定或自责的想法" prop="q8">
            <el-radio-group v-model="form.q8">
              <el-radio label="2">完全没有</el-radio>
              <el-radio label="1">偶尔会有</el-radio>
              <el-radio label="0">经常出现</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item style="text-align: center; margin-top: 30px">
            <el-button type="primary" @click="submitPaper" :loading="loading">
              提交问卷并进入预约
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>

    <!-- 3. 预约页面【改版：宽屏列表样式，对标管理员用户管理页面】 -->
    <div v-if="step === 3" class="book-box">
      <el-card shadow="hover">
        <div
          class="title"
          style="
            font-size: 22px;
            font-weight: bold;
            padding: 12px 0;
            border-bottom: 1px solid #eee;
            margin-bottom: 20px;
          "
        >
          心理咨询预约
        </div>

        <!-- ====================== 【新增：预约地点下拉框】 ====================== -->
        <el-form-item label="选择预约地点" label-width="120px" style="margin-bottom:20px">
          <el-select v-model="form.location" placeholder="请选择预约地点" style="width:300px">
            <el-option label="心理咨询室" value="心理咨询室" />
            <el-option label="一号教室" value="一号教室" />
            <el-option label="二号教室" value="二号教室" />
            <el-option label="三号教室" value="三号教室" />
            <el-option label="线上咨询" value="线上咨询" />
          </el-select>
        </el-form-item>
        <!-- ===================================================================== -->

        <!-- 列表头部 -->
        <div class="list-head">
          <div class="col-name">初访员姓名</div>
          <div class="col-date">值班日期</div>
          <div class="col-time">值班时段</div>
          <div class="col-op">操作</div>
        </div>
        <!-- 数据条目，一条一行 -->
        <div
          class="list-item"
          v-for="item in dutyList"
          :key="item.id"
          :class="{ active: form.counselorId === item.userId }"
          @click="form.counselorId = item.userId"
        >
          <div class="col-name">{{ item.userName }}</div>
          <div class="col-date">{{ item.dutyDate }}</div>
          <div class="col-time">{{ item.startTime }} ~ {{ item.endTime }}</div>
          <div class="col-op">
            <el-button size="small" type="primary" plain>选择预约</el-button>
          </div>
        </div>

        <el-button
          type="primary"
          style="width: 100%; margin-top: 24px; height: 42px"
          @click="submitAppoint"
        >
          提交预约申请
        </el-button>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import request from "@/utils/request";

const router = useRouter();

// 步骤控制：1=同意书 2=问卷 3=预约
const step = ref(1);
const showAgreement = ref(true);
const isAgreed = ref(false);
const loading = ref(false);

// ====================== 【保存问卷结果】 ======================
const questionResult = ref(null)

const formRef = ref(null);
const form = reactive({
  q1: "",
  q2: "",
  q3: "",
  q4: "",
  q5: "",
  q6: "",
  q7: "",
  q8: "",
  counselorId: "",
  location: ""  // 新增地点
});

const rules = {
  q1: [{ required: true, message: "第1题未作答", trigger: "change" }],
  q2: [{ required: true, message: "第2题未作答", trigger: "change" }],
  q3: [{ required: true, message: "第3题未作答", trigger: "change" }],
  q4: [{ required: true, message: "第4题未作答", trigger: "change" }],
  q5: [{ required: true, message: "第5题未作答", trigger: "change" }],
  q6: [{ required: true, message: "第6题未作答", trigger: "change" }],
  q7: [{ required: true, message: "第7题未作答", trigger: "change" }],
  q8: [{ required: true, message: "第8题未作答", trigger: "change" }],
};

const dutyList = ref([]);
const loadDutyList = async () => {
  try {
    // 使用学生专用接口，后端已过滤只返回初访员(role=visitor)排班
    const res = await request.get("/student/public/duty/list");
    dutyList.value = res.data || [];
  } catch (err) {
    console.error("❌ 获取失败：", err);
    ElMessage.error("获取初访员值班信息失败");
  }
};

// ====================== 【修复：只提交问卷算分，不创建预约】 ======================
import { nextTick } from 'vue'

const submitPaper = async () => {
  loading.value = true
  // 改用Promise写法校验，抛弃回调嵌套（根治async失效）
  let valid
  try {
    valid = await formRef.value.validate()
  } catch {
    loading.value = false
    return
  }
  if (!valid) {
    loading.value = false
    return
  }

  const answerList = [
    Number(form.q1),Number(form.q2),Number(form.q3),Number(form.q4),
    Number(form.q5),Number(form.q6),Number(form.q7),Number(form.q8)
  ];
  try {
    const userInfo = JSON.parse(localStorage.getItem('user'));
    const res = await request.post("/student/saveQuestion", answerList, {
      params: { studentId: userInfo.userId }
    });
    questionResult.value = res.data;
    ElMessage.success(`问卷提交成功！您的测评分数：${res.data.score} 分，结果：${res.data.level}`);

    step.value = 3
    // 关键点：等待step切换DOM渲染完毕，再清空+请求
    await nextTick()
    dutyList.value = []
    await loadDutyList()

  } catch (e) {
    console.error(e)
    ElMessage.error("问卷提交失败");
  }
  loading.value = false;
};

const goBack = () => router.back();
const agreeToContinue = () => {
  showAgreement.value = false;
  step.value = 2;
  ElMessage.success("已进入评估问卷");
};

// ====================== 【修复：提交预约时，把问卷分数一起存入数据库】 ======================
const submitAppoint = async () => {
  if (!form.counselorId) return ElMessage.warning("请选择初访员");
  if (!form.location) return ElMessage.warning("请选择预约地点");
  if (!questionResult.value) return ElMessage.warning("请先完成问卷");
  
  const c = dutyList.value.find((i) => i.userId === form.counselorId);
  if(!c){
    ElMessage.error("未匹配到值班数据");
    return;
  }
  const userInfo = JSON.parse(localStorage.getItem('user'));
  loading.value = true
  
  try {
    // 提交预约 + 问卷分数一起入库
    await request.post("/student/addVisit", {
      studentId: userInfo.userId,
      visitorId: form.counselorId,
      dutyId: c.id,
      location: form.location,
      status: "待审核",
      createTime: new Date(),
      applyTime: new Date()
    }, {
      params: {
        score: questionResult.value.score,
        isAlert: questionResult.value.isAlert
      }
    })
    
    ElMessage.success(`预约提交成功！\n初访员：${c.userName}\n地点：${form.location}`);
    setTimeout(()=>router.push('/student'),2000)
  }catch(e){
    console.error(e)
    ElMessage.error('预约保存失败')
  }finally{loading.value=false}
}
</script>

<style scoped>
.page-container {
  padding: 24px;
  width: 100%;
  min-height: 100vh;
  background: #f5f7fa;
}
/* 问卷盒子取消窄宽度，铺满大部分页面 */
.paper-box {
  max-width: 100%;
}
/* 预约盒子取消800px限制，全屏宽，对标管理员页面 */
.book-box {
  max-width: 100%;
}
.form-card {
  padding: 24px;
}
.paper-title {
  font-size: 24px;
  font-weight: bold;
  text-align: center;
  margin-bottom: 10px;
}
.paper-desc {
  text-align: center;
  color: #666;
  margin: 0 0 35px;
  font-size: 15px;
}
.agreement-box {
  line-height: 1.8;
  padding: 10px 20px;
}
.dialog-footer {
  text-align: right;
}

/* 问卷表单样式 */
.paper-form :deep(.el-form-item) {
  display: flex !important;
  align-items: center !important;
  margin-bottom: 26px !important;
}
.paper-form :deep(.el-form-item__label-wrap) {
  width: 360px !important;
  min-width: 360px !important;
  flex: none !important;
}
.paper-form :deep(.el-form-item__label) {
  text-align: left !important;
  text-indent: 0 !important;
  line-height: 1.6;
  font-size: 15px;
}
.paper-form :deep(.el-form-item__content) {
  flex: 1 !important;
}
.paper-form :deep(.el-radio-group) {
  display: flex;
  justify-content: space-between;
  width: 100%;
}
.paper-form :deep(.el-radio) {
  width: 32%;
  display: flex;
  align-items: center;
  margin: 0 !important;
}

/* ========== 预约列表样式【对标管理员表格布局】 ========== */
.list-head {
  display: grid;
  grid-template-columns: 220px 220px 320px 160px;
  padding: 12px 16px;
  background: #f5f7fa;
  font-weight: bold;
  border: 1px solid #e5e7eb;
}
.list-item {
  display: grid;
  grid-template-columns: 220px 220px 320px 160px;
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-top: none;
  cursor: pointer;
  transition: 0.2s;
}
.list-item:hover {
  background: #f0f7ff;
}
.list-item.active {
  background: #e8f3ff;
  border-color: #409eff;
}
.col-name,
.col-date,
.col-time,
.col-op {
  display: flex;
  align-items: center;
}
</style>