import type { Component } from 'vue'

export interface WorkspaceSidebarChildItem {
  key: string
  label: string
  disabled?: boolean
}

export interface WorkspaceSidebarItem {
  key: string
  label: string
  icon?: Component
  disabled?: boolean
  children?: WorkspaceSidebarChildItem[]
}

export interface WorkspaceTabItem {
  name: string
  label: string
  disabled?: boolean
  badge?: string | number
}
