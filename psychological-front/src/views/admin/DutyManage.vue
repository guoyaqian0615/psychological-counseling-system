<template>
  <div style="padding: 20px;">

    <!-- 筛选栏 -->
    <el-card style="margin-bottom: 12px;">
      <el-row :gutter="12" align="middle">
        <el-col :span="6">
          <el-date-picker
            v-model="filterDate"
            type="month"
            placeholder="筛选月份"
            value-format="YYYY-MM"
            clearable
            @change="onFilterChange"
          />
        </el-col>
        <el-col :span="6">
          <el-select v-model="filterRole" placeholder="角色筛选" clearable @change="onFilterChange">
            <el-option label="全部" value="" />
            <el-option label="仅初访员" value="visitor" />
            <el-option label="仅咨询师" value="counselor" />
          </el-select>
        </el-col>
        <el-col :span="12" style="text-align: right;">
          <el-button type="primary" @click="openAdd">+ 新增排班</el-button>
          <el-button type="success" @click="openAutoGenerate" style="margin-left:8px">
            自动生成排班
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- ============================================================
         初访员排班区域：按人员分组，每组内按日期升序展示
         ============================================================ -->
    <template v-if="!filterRole || filterRole === 'visitor'">
      <el-card style="margin-bottom: 16px;">
        <template #header>
          <div style="display:flex; align-items:center; gap:8px;">
            <el-tag type="primary" effect="dark">初访员排班</el-tag>
            <span style="color:#606266; font-size:13px;">按值班日期排列，每条记录即一个独立时段</span>
          </div>
        </template>

        <div v-if="visitorGroups.length === 0 && !loading" class="empty-hint">
          暂无初访员排班数据
        </div>

        <div
          v-for="group in visitorGroups"
          :key="group.userId"
          class="person-group"
        >
          <!-- 人员标题行 -->
          <div class="group-header">
            <span class="group-name">{{ group.userName }}</span>
            <el-tag type="primary" size="small">初访员</el-tag>
            <span class="group-count">共 {{ group.records.length }} 个时段</span>
          </div>

          <!-- 该初访员的所有值班条目 -->
          <el-table
            :data="group.records"
            border
            size="small"
            style="width:100%; margin-bottom:4px;"
            :show-header="group === visitorGroups[0]"
          >
            <el-table-column prop="dutyDate" label="值班日期" width="130">
              <template #default="scope">
                <span :class="isPast(scope.row.dutyDate) ? 'date-past' : 'date-future'">
                  {{ scope.row.dutyDate }}
                </span>
                <el-tag v-if="isToday(scope.row.dutyDate)" type="danger" size="small" style="margin-left:4px">今天</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="值班时段" width="160">
              <template #default="scope">
                {{ scope.row.startTime?.slice(0,5) }} ~ {{ scope.row.endTime?.slice(0,5) }}
              </template>
            </el-table-column>
            <el-table-column prop="maxPerson" label="最多预约人数" width="120" align="center">
              <template #default="scope">
                <el-tag type="info">{{ scope.row.maxPerson ?? '-' }} 人</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" show-overflow-tooltip />
            <el-table-column label="状态" width="90" align="center">
              <template #default="scope">
                <el-tag :type="scope.row.status === 'locked' ? 'danger' : 'success'" size="small">
                  {{ scope.row.status === 'locked' ? '锁定' : '正常' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="130" fixed="right" align="center">
              <template #default="scope">
                <el-button size="small" type="primary" plain @click="openEdit(scope.row)">调整</el-button>
                <el-button size="small" type="danger"  plain @click="del(scope.row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 初访员分页 -->
        <div style="margin-top:12px; text-align:right;" v-if="visitorTotal > visitorPageSize">
          <el-pagination
            v-model:current-page="visitorPage"
            v-model:page-size="visitorPageSize"
            :total="visitorTotal"
            :page-sizes="[5, 10, 20]"
            layout="total, sizes, prev, pager, next"
            @change="loadVisitorPage"
          />
        </div>
        <div v-else style="margin-top:6px; color:#c0c4cc; font-size:12px; text-align:right;">
          共 {{ visitorTotal }} 条初访员排班记录
        </div>
      </el-card>
    </template>

    <!-- ============================================================
         咨询师排班区域：以8周为一个值班周期，展示基准日（第一天），
         可展开查看完整8周日期列表
         ============================================================ -->
    <template v-if="!filterRole || filterRole === 'counselor'">
      <el-card>
        <template #header>
          <div style="display:flex; align-items:center; gap:8px;">
            <el-tag type="success" effect="dark">咨询师排班</el-tag>
            <span style="color:#606266; font-size:13px;">
              以8周为一个值班周期，下方展示每位咨询师的排班周期概览
            </span>
          </div>
        </template>

        <div v-if="counselorCycles.length === 0 && !loading" class="empty-hint">
          暂无咨询师排班数据
        </div>

        <!-- 每位咨询师一个周期卡片 -->
        <div
          v-for="cycle in counselorCycles"
          :key="cycle.userId + '-' + cycle.startTime"
          class="counselor-cycle-card"
        >
          <!-- 周期概览行（可点击展开） -->
          <div class="cycle-header" @click="toggleCycle(cycle.key)">
            <div class="cycle-left">
              <span class="group-name">{{ cycle.userName }}</span>
              <el-tag type="success" size="small">咨询师</el-tag>
              <span class="cycle-time-badge">
                {{ cycle.startTime?.slice(0,5) }} ~ {{ cycle.endTime?.slice(0,5) }}
              </span>
              <el-tag type="info" size="small">每 {{ cycle.maxPerson }} 人/场</el-tag>
            </div>
            <div class="cycle-right">
              <span class="cycle-range">
                📅 周期基准日：<b>{{ cycle.baseDate }}</b>
                &nbsp;·&nbsp;共 {{ cycle.weeks.length }} 周
                &nbsp;·&nbsp;最后一天：{{ cycle.lastDate }}
              </span>
              <el-icon style="margin-left:8px; color:#909399;">
                <component :is="expandedCycles.has(cycle.key) ? ArrowUp : ArrowDown" />
              </el-icon>
            </div>
          </div>

          <!-- 展开的周明细 -->
          <el-collapse-transition>
            <div v-if="expandedCycles.has(cycle.key)" class="cycle-detail">
              <el-table
                :data="cycle.weeks"
                border
                size="small"
                style="width:100%;"
              >
                <el-table-column label="周次" width="70" align="center">
                  <template #default="scope">
                    <span style="color:#909399; font-size:12px;">第 {{ scope.$index + 1 }} 周</span>
                  </template>
                </el-table-column>
                <el-table-column prop="dutyDate" label="值班日期" width="130">
                  <template #default="scope">
                    <span :class="isPast(scope.row.dutyDate) ? 'date-past' : 'date-future'">
                      {{ scope.row.dutyDate }}
                    </span>
                    <el-tag v-if="isToday(scope.row.dutyDate)" type="danger" size="small" style="margin-left:4px">今天</el-tag>
                    <el-tag v-if="scope.$index === 0" type="warning" size="small" style="margin-left:4px">基准日</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="值班时段" width="160">
                  <template #default="scope">
                    {{ scope.row.startTime?.slice(0,5) }} ~ {{ scope.row.endTime?.slice(0,5) }}
                  </template>
                </el-table-column>
                <el-table-column prop="maxPerson" label="最多预约" width="100" align="center">
                  <template #default="scope">
                    <el-tag type="info" size="small">{{ scope.row.maxPerson ?? '-' }} 人</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="状态" width="80" align="center">
                  <template #default="scope">
                    <el-tag :type="scope.row.status === 'locked' ? 'danger' : 'success'" size="small">
                      {{ scope.row.status === 'locked' ? '锁定' : '正常' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="remark" label="备注" show-overflow-tooltip />
                <el-table-column label="操作" width="130" fixed="right" align="center">
                  <template #default="scope">
                    <el-button size="small" type="primary" plain @click="openEdit(scope.row)">调整</el-button>
                    <el-button size="small" type="danger"  plain @click="del(scope.row.id)">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </el-collapse-transition>
        </div>

        <!-- 咨询师分页 -->
        <div style="margin-top:12px; text-align:right;" v-if="counselorTotal > counselorPageSize">
          <el-pagination
            v-model:current-page="counselorPage"
            v-model:page-size="counselorPageSize"
            :total="counselorTotal"
            :page-sizes="[5, 10, 20]"
            layout="total, sizes, prev, pager, next"
            @change="loadCounselorPage"
          />
        </div>
        <div v-else style="margin-top:6px; color:#c0c4cc; font-size:12px; text-align:right;">
          共 {{ counselorTotal }} 条咨询师排班记录
        </div>
      </el-card>
    </template>

    <!-- ==================== 新增 / 调整弹窗 ==================== -->
    <el-dialog
      v-model="show"
      :title="form.id ? '调整排班' : '新增排班'"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
        <el-form-item label="选择老师" prop="userId">
          <el-select v-model="form.userId" placeholder="请选择老师" style="width:100%" filterable>
            <el-option-group label="初访员">
              <el-option v-for="t in visitorTeachers" :key="t.id" :label="t.name" :value="t.id">
                <span>{{ t.name }}</span>
                <el-tag type="primary" size="small" style="margin-left:8px">初访员</el-tag>
              </el-option>
            </el-option-group>
            <el-option-group label="咨询师">
              <el-option v-for="t in counselorTeachers" :key="t.id" :label="t.name" :value="t.id">
                <span>{{ t.name }}</span>
                <el-tag type="success" size="small" style="margin-left:8px">咨询师</el-tag>
              </el-option>
            </el-option-group>
          </el-select>
        </el-form-item>

        <el-form-item label="值班日期" prop="dutyDate">
          <el-date-picker
            v-model="form.dutyDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择值班日期"
            style="width:100%"
          />
        </el-form-item>

        <el-form-item label="开始时间" prop="startTime">
          <el-time-picker
            v-model="form.startTime"
            value-format="HH:mm:ss"
            placeholder="选择开始时间"
            style="width:100%"
            @change="handleStartTimeChange"
          />
          <div style="color:#999;font-size:12px;margin-top:4px;">
            <span v-if="timeConfig">按配置时长 {{ timeConfig.singleDuration }} 分钟自动计算结束时间</span>
          </div>
        </el-form-item>

        <el-form-item label="结束时间" prop="endTime">
          <el-time-picker
            v-model="form.endTime"
            value-format="HH:mm:ss"
            placeholder="选择结束时间"
            style="width:100%"
            @change="() => formRef?.validateField('endTime')"
          />
          <div style="color:#999;font-size:12px;margin-top:4px;">
            <span v-if="timeConfig">有效时间范围：{{ String(timeConfig.dailyStartHour).padStart(2,'0') }}:00 ~ {{ String(timeConfig.dailyEndHour).padStart(2,'0') }}:00</span>
          </div>
        </el-form-item>

        <el-form-item label="最多预约人数" prop="maxPerson">
          <el-input-number v-model="form.maxPerson" :min="1" :max="20" controls-position="right" style="width:100%" />
          <div style="color:#999;font-size:12px;margin-top:4px;">每个时间段最多可预约的学生数量</div>
        </el-form-item>

        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio value="normal">正常排班</el-radio>
            <el-radio value="locked">锁定（暂停预约）</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="如：临时调整说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="show = false">取消</el-button>
        <el-button type="primary" @click="save" :loading="saving">保存排班</el-button>
      </template>
    </el-dialog>

    <!-- ==================== 自动生成排班弹窗 ==================== -->
    <el-dialog
      v-model="showAuto"
      title="自动生成排班（按时间配置）"
      width="420px"
      :close-on-click-modal="false"
    >
      <el-form :model="autoForm" ref="autoFormRef" label-width="110px" :rules="autoRules">
        <el-form-item label="选择老师" prop="userId">
          <el-select v-model="autoForm.userId" style="width:100%" filterable>
            <el-option-group label="初访员">
              <el-option v-for="t in visitorTeachers" :key="t.id" :label="t.name" :value="t.id">
                <span>{{ t.name }}</span>
                <el-tag type="primary" size="small" style="margin-left:8px">初访员</el-tag>
              </el-option>
            </el-option-group>
            <el-option-group label="咨询师">
              <el-option v-for="t in counselorTeachers" :key="t.id" :label="t.name" :value="t.id">
                <span>{{ t.name }}</span>
                <el-tag type="success" size="small" style="margin-left:8px">咨询师</el-tag>
              </el-option>
            </el-option-group>
          </el-select>
        </el-form-item>
        <el-form-item label="排班日期" prop="dutyDate">
          <el-date-picker
            v-model="autoForm.dutyDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width:100%"
            placeholder="选择日期"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAuto = false">取消</el-button>
        <el-button type="primary" @click="doAutoGenerate" :loading="generating">一键生成排班</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive } from 'vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown, ArrowUp } from '@element-plus/icons-vue'

// ========================== 基础状态 ==========================
const allTeachers  = ref([])
const loading      = ref(false)
const show         = ref(false)
const saving       = ref(false)
const formRef      = ref(null)
const timeConfig   = ref(null)

const filterDate = ref('')
const filterRole = ref('')

// ========================== 初访员分页相关 ==========================
// 存储从后端拉取的所有初访员排班数据（已去重处理）
const allVisitorRecords = ref([])
const visitorPage     = ref(1)
const visitorPageSize = ref(10)
const visitorTotal    = ref(0)

/**
 * 初访员分组展示：按人员分组，组内按 dutyDate 升序。
 * 每页 visitorPageSize 条原始记录，跨人员分组。
 */
const visitorGroups = computed(() => {
  const start = (visitorPage.value - 1) * visitorPageSize.value
  const end   = start + visitorPageSize.value
  const pageRecords = allVisitorRecords.value.slice(start, end)

  // 按 userId 分组，保持顺序
  const map = new Map()
  for (const r of pageRecords) {
    if (!map.has(r.userId)) {
      map.set(r.userId, { userId: r.userId, userName: r.userName, records: [] })
    }
    map.get(r.userId).records.push(r)
  }
  return [...map.values()]
})

const loadVisitorPage = () => {
  // 数据已全量加载到 allVisitorRecords，切页只是 computed 重新切片，无需请求
}

// ========================== 咨询师分页相关 ==========================
/**
 * 咨询师周期（cycle）定义：
 *  同一人、同一时段（startTime+endTime）、相邻相差7天的连续记录 → 为一个周期组。
 *  每个周期只展示首行（基准日），点击可展开查看所有周次。
 */
const allCounselorCycles = ref([])   // 所有周期（已聚合）
const counselorPage     = ref(1)
const counselorPageSize = ref(5)     // 每页展示5个周期（每个周期代表一位老师的一个时段组）
const counselorTotal    = ref(0)

const counselorCycles = computed(() => {
  const start = (counselorPage.value - 1) * counselorPageSize.value
  const end   = start + counselorPageSize.value
  return allCounselorCycles.value.slice(start, end)
})

const loadCounselorPage = () => {
  // 纯前端切页
}

// 展开状态：Set of cycle.key
const expandedCycles = reactive(new Set())
const toggleCycle = (key) => {
  if (expandedCycles.has(key)) expandedCycles.delete(key)
  else expandedCycles.add(key)
}

// ========================== 弹窗表单 ==========================
const form = ref({})
const showAuto    = ref(false)
const generating  = ref(false)
const autoFormRef = ref(null)
const autoForm    = ref({ userId: null, dutyDate: '' })
const autoRules   = ref({
  userId:    [{ required: true, message: '请选择老师', trigger: 'change' }],
  dutyDate:  [{ required: true, message: '请选择日期', trigger: 'change' }],
})

const rules = {
  userId:    [{ required: true, message: '请选择值班老师', trigger: 'change' }],
  dutyDate:  [{ required: true, message: '请选择值班日期', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [
    { required: true, message: '请选择结束时间', trigger: 'change' },
    {
      validator: (rule, value, callback) => {
        if (form.value.startTime && value <= form.value.startTime) {
          callback(new Error('结束时间必须晚于开始时间'))
        } else { callback() }
      },
      trigger: 'change'
    }
  ],
  maxPerson: [{ required: true, message: '请设置最多预约人数', trigger: 'blur' }],
}

const visitorTeachers = computed(() => allTeachers.value.filter(t => t.role === 'visitor'))
const counselorTeachers = computed(() => allTeachers.value.filter(t => t.role === 'counselor'))

// ========================== 日期工具 ==========================
const todayStr = new Date().toISOString().slice(0, 10)
const isToday  = (d) => d === todayStr
const isPast   = (d) => d < todayStr

// ========================== 咨询师周期聚合 ==========================
/**
 * 将咨询师原始 records（已按 userId, startTime, dutyDate 排序）聚合成周期组。
 * 规则：同一 userId + startTime + endTime + maxPerson 的记录，
 *       按 dutyDate 升序排列，相邻日期差 ≤ 8 天（允许7天一周的误差）视为同一周期。
 */
const buildCounselorCycles = (records) => {
  // 先按 userId → startTime → dutyDate 排序
  const sorted = [...records].sort((a, b) => {
    if (a.userId !== b.userId) return a.userId - b.userId
    const tA = (a.startTime || '').slice(0, 5)
    const tB = (b.startTime || '').slice(0, 5)
    if (tA !== tB) return tA.localeCompare(tB)
    return (a.dutyDate || '').localeCompare(b.dutyDate || '')
  })

  const cycles = []
  let current = null

  for (const r of sorted) {
    const key = `${r.userId}_${(r.startTime || '').slice(0,5)}_${(r.endTime || '').slice(0,5)}`
    if (!current || current.key !== key) {
      // 新周期组
      current = {
        key,
        userId:    r.userId,
        userName:  r.userName,
        startTime: r.startTime,
        endTime:   r.endTime,
        maxPerson: r.maxPerson,
        baseDate:  r.dutyDate,
        lastDate:  r.dutyDate,
        weeks:     [r],
      }
      cycles.push(current)
    } else {
      // 检查是否与上一条相差约7天（属于同一周期）
      const prevDate = new Date(current.lastDate)
      const curDate  = new Date(r.dutyDate)
      const diffDays = Math.round((curDate - prevDate) / 86400000)
      if (diffDays >= 5 && diffDays <= 10) {
        // 同一周期延续
        current.weeks.push(r)
        current.lastDate = r.dutyDate
      } else {
        // 新的周期组（时间跳跃，另起一组）
        current = {
          key: key + '_' + r.dutyDate,
          userId:    r.userId,
          userName:  r.userName,
          startTime: r.startTime,
          endTime:   r.endTime,
          maxPerson: r.maxPerson,
          baseDate:  r.dutyDate,
          lastDate:  r.dutyDate,
          weeks:     [r],
        }
        cycles.push(current)
      }
    }
  }
  return cycles
}

// ========================== 数据加载 ==========================
const getList = async () => {
  loading.value = true
  try {
    // 一次性拉取所有排班数据用于前端分组（利用后端大 pageSize）
    const res = await request.get('/admin/duty/page', {
      params: { pageNum: 1, pageSize: 500 }
    })
    const pageData = res.data
    const records = pageData.records ?? pageData

    // 给每条记录补充 userName / userRole
    records.forEach(d => {
      const teacher = allTeachers.value.find(t => t.id === d.userId)
      if (teacher) {
        d.userName = teacher.name
        d.userRole = teacher.role
      }
      if (d.status === 0 || d.status === '0') d.status = 'normal'
      if (d.status === 1 || d.status === '1') d.status = 'locked'
    })

    // --- 初访员：筛选 + 按日期升序 ---
    let visitorRaw = records
      .filter(d => d.userRole === 'visitor')
      .sort((a, b) => {
        // 先按人员名，再按日期升序
        if (a.userId !== b.userId) return (a.userName || '').localeCompare(b.userName || '', 'zh')
        return (a.dutyDate || '').localeCompare(b.dutyDate || '')
      })

    // 月份筛选
    if (filterDate.value) {
      visitorRaw = visitorRaw.filter(d => d.dutyDate?.startsWith(filterDate.value))
    }

    allVisitorRecords.value = visitorRaw
    visitorTotal.value      = visitorRaw.length
    visitorPage.value       = 1

    // --- 咨询师：聚合成周期组 ---
    let counselorRaw = records.filter(d => d.userRole === 'counselor')
    if (filterDate.value) {
      counselorRaw = counselorRaw.filter(d => d.dutyDate?.startsWith(filterDate.value))
    }

    const cycles = buildCounselorCycles(counselorRaw)
    allCounselorCycles.value = cycles
    counselorTotal.value     = cycles.length
    counselorPage.value      = 1

  } catch (e) {
    ElMessage.error('获取排班列表失败')
    console.error(e)
  } finally {
    loading.value = false
  }
}

const onFilterChange = () => {
  getList()
}

const getTeachers = async () => {
  try {
    const res = await request.get('/admin/user/list')
    allTeachers.value = (res.data || []).filter(u =>
      u.role === 'counselor' || u.role === 'visitor'
    )
  } catch (e) {
    console.error(e)
  }
}

const getTimeConfig = async () => {
  try {
    const res = await request.get('/admin/timeConfig/get')
    timeConfig.value = res.data
  } catch (e) {
    console.error('获取时间配置失败:', e)
    ElMessage.warning('无法获取时间配置，请先在系统设置中配置')
  }
}

// ========================== 时间计算 ==========================
const calculateEndTime = (startTimeStr) => {
  if (!startTimeStr || !timeConfig.value) return null
  try {
    const [h, m, s] = startTimeStr.split(':').map(Number)
    const start = new Date(2000, 0, 1, h, m, s || 0)
    const duration = timeConfig.value.singleDuration || 30
    const end = new Date(start.getTime() + duration * 60000)
    const endTotal = end.getHours() * 60 + end.getMinutes()
    const limit    = (timeConfig.value.dailyEndHour || 18) * 60
    if (endTotal > limit) {
      ElMessage.warning(`超过每日结束时间 ${timeConfig.value.dailyEndHour}:00，请调整`)
      return null
    }
    return [
      String(end.getHours()).padStart(2, '0'),
      String(end.getMinutes()).padStart(2, '0'),
      String(end.getSeconds()).padStart(2, '0'),
    ].join(':')
  } catch (e) { return null }
}

const handleStartTimeChange = () => {
  if (form.value.startTime) {
    const et = calculateEndTime(form.value.startTime)
    if (et) {
      form.value.endTime = et
      formRef.value?.validateField('endTime')
    }
  }
}

// ========================== CRUD ==========================
const openAdd = () => {
  form.value = { userId: null, dutyDate: '', startTime: '', endTime: '', maxPerson: 3, status: 'normal', remark: '' }
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
    if (!form.value.startTime || !timeConfig.value) {
      ElMessage.warning('请选择开始时间或检查时间配置')
      return
    }
    const hh = parseInt(form.value.startTime.split(':')[0])
    if (hh < timeConfig.value.dailyStartHour || hh > timeConfig.value.dailyEndHour) {
      ElMessage.error(`开始时间必须在 ${timeConfig.value.dailyStartHour}:00 ~ ${timeConfig.value.dailyEndHour}:00 之间`)
      return
    }
    saving.value = true
    try {
      const payload = { ...form.value }
      payload.status = payload.status === 'normal' ? '0' : '1'
      const res = await request.post('/admin/duty/save', payload)
      const ok = res && (res.code === 200 || res.code === 0 || res.code === '200' || res.code === '0')
      if (!ok) { ElMessage.error((res && res.msg) || '保存失败'); return }
      ElMessage.success(form.value.id ? '排班调整成功' : '保存成功')
      show.value = false
      getList()
    } catch (e) {
      ElMessage.error(e.response?.data?.msg || '保存失败，请检查时间是否冲突')
    } finally {
      saving.value = false
    }
  })
}

const del = async (id) => {
  try {
    await ElMessageBox.confirm('确认删除该排班记录？', '提示', { type: 'warning' })
    await request.delete(`/admin/duty/${id}`)
    ElMessage.success('删除成功')
    getList()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

const openAutoGenerate = () => {
  autoForm.value = { userId: null, dutyDate: '' }
  showAuto.value = true
  autoFormRef.value?.clearValidate()
}

const doAutoGenerate = async () => {
  await autoFormRef.value.validate(async valid => {
    if (!valid) return
    generating.value = true
    try {
      const res = await request.post('/admin/duty/auto', null, { params: autoForm.value })
      ElMessage.success(`生成成功，共 ${res.data?.length ?? 0} 个时段`)
      showAuto.value = false
      getList()
    } catch (e) {
      ElMessage.error(e.response?.data?.msg || '生成失败')
    } finally {
      generating.value = false
    }
  })
}

onMounted(async () => {
  await getTeachers()
  await getTimeConfig()
  getList()
})
</script>

<style scoped>
/* 通用空状态 */
.empty-hint {
  text-align: center;
  padding: 32px;
  color: #c0c4cc;
  font-size: 14px;
}

/* ========== 初访员分组 ========== */
.person-group {
  margin-bottom: 12px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  overflow: hidden;
}

.group-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
}

.group-name {
  font-weight: 600;
  color: #303133;
  font-size: 14px;
}

.group-count {
  margin-left: auto;
  font-size: 12px;
  color: #909399;
}

/* ========== 咨询师周期卡片 ========== */
.counselor-cycle-card {
  margin-bottom: 12px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  overflow: hidden;
}

.cycle-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  background: #f0f9eb;
  border-bottom: 1px solid #e4e7ed;
  cursor: pointer;
  user-select: none;
  transition: background .15s;
}
.cycle-header:hover { background: #e8f5e0; }

.cycle-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.cycle-time-badge {
  background: #ecf5ff;
  color: #409eff;
  border: 1px solid #b3d8ff;
  border-radius: 4px;
  padding: 1px 8px;
  font-size: 13px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.cycle-right {
  display: flex;
  align-items: center;
  color: #606266;
  font-size: 13px;
}

.cycle-range { color: #606266; }

.cycle-detail {
  padding: 8px;
  background: #fafafa;
}

/* ========== 日期样式 ========== */
.date-past   { color: #c0c4cc; }
.date-future { color: #303133; font-weight: 500; }
</style>
