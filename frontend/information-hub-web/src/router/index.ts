import {
  createRouter,
  createWebHistory,
  type Router,
  type RouterHistory,
} from 'vue-router'

import JobDetailView from '@/views/JobDetailView.vue'
import JobSnapshotsView from '@/views/JobSnapshotsView.vue'
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
        component: JobDetailView,
      },
      {
        path: '/jobs/:id/snapshots',
        name: 'job-snapshots',
        component: JobSnapshotsView,
      },
      {
        path: '/:pathMatch(.*)*',
        name: 'not-found',
        component: NotFoundView,
      },
    ],
  })
}
