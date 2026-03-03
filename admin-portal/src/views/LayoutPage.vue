<template>
  <el-container class="layout-container">
    <el-aside width="220px">
      <div class="logo">AI漫画小说管理端</div>
      <el-menu
        :default-active="route.path"
        router
        background-color="#001529"
        text-color="#ffffffb3"
        active-text-color="#409eff"
      >
        <el-menu-item index="/storyline">
          <el-icon><Document /></el-icon>
          <span>故事线管理</span>
        </el-menu-item>
        <el-menu-item index="/content">
          <el-icon><Files /></el-icon>
          <span>内容管理</span>
        </el-menu-item>
        <el-menu-item index="/paid">
          <el-icon><Money /></el-icon>
          <span>付费管理</span>
        </el-menu-item>
        <el-sub-menu index="analytics">
          <template #title>
            <el-icon><DataAnalysis /></el-icon>
            <span>数据统计</span>
          </template>
          <el-menu-item index="/analytics/user">用户使用统计</el-menu-item>
          <el-menu-item index="/analytics/token">Token消耗看板</el-menu-item>
          <el-menu-item index="/analytics/recharge">充值统计看板</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header>
        <span class="page-title">{{ route.meta.title }}</span>
        <el-dropdown @command="handleCommand">
          <span class="admin-info">
            {{ authStore.admin?.nickname || '管理员' }}
            <el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { Document, Files, Money, DataAnalysis, ArrowDown } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

function handleCommand(command: string) {
  if (command === 'logout') {
    authStore.clearAuth()
    router.push({ name: 'Login' })
  }
}
</script>
