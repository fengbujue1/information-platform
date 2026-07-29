import {
  createRouter,
  createWebHistory,
  type Router,
  type RouterHistory,
} from 'vue-router'

import JobsPlaceholderView from '@/views/JobsPlaceholderView.vue'
import NotFoundView from '@/views/NotFoundView.vue'

export function createAppRouter(
  history: RouterHistory = createWebHistory(),
): Router {
  return createRouter({
    history,
    routes: [
      {
        path: '/',
        redirect: '/jobs',
      },
      {
        path: '/jobs',
        name: 'jobs',
        component: JobsPlaceholderView,
      },
      {
        path: '/:pathMatch(.*)*',
        name: 'not-found',
        component: NotFoundView,
      },
    ],
  })
}
