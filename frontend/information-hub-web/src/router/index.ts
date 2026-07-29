import {
  createRouter,
  createWebHistory,
  type Router,
  type RouterHistory,
} from 'vue-router'

import JobDetailPlaceholderView from '@/views/JobDetailPlaceholderView.vue'
import JobsView from '@/views/JobsView.vue'
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
        component: JobsView,
      },
      {
        path: '/jobs/:id',
        name: 'job-detail',
        component: JobDetailPlaceholderView,
      },
      {
        path: '/:pathMatch(.*)*',
        name: 'not-found',
        component: NotFoundView,
      },
    ],
  })
}
