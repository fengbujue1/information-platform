import {
  createRouter,
  createWebHistory,
  type Router,
  type RouterHistory,
} from 'vue-router'

import { setUnauthorizedHandler } from '@/api/httpClient'
import {
  clearAuthentication,
  ensureCurrentUser,
} from '@/stores/authSession'
import type { CurrentUser } from '@/types/identity'
import JobDetailView from '@/views/JobDetailView.vue'
import JobSnapshotsView from '@/views/JobSnapshotsView.vue'
import JobsView from '@/views/JobsView.vue'
import LoginView from '@/views/LoginView.vue'
import NotFoundView from '@/views/NotFoundView.vue'

type CurrentUserResolver = () => Promise<CurrentUser | null>

export function createAppRouter(
  history: RouterHistory = createWebHistory(),
  currentUserResolver: CurrentUserResolver = ensureCurrentUser,
): Router {
  const router = createRouter({
    history,
    routes: [
      {
        path: '/',
        redirect: '/jobs',
      },
      {
        path: '/login',
        name: 'login',
        component: LoginView,
        meta: { public: true },
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

  setUnauthorizedHandler((requestUrl) => {
    if (
      requestUrl.includes('/v1/auth/login') ||
      requestUrl.includes('/v1/auth/me')
    ) {
      return
    }
    clearAuthentication()
    const currentPath = router.currentRoute.value.fullPath
    void router.replace({
      name: 'login',
      query: { redirect: currentPath },
    })
  })

  router.beforeEach(async (to) => {
    const user = await currentUserResolver()
    if (to.meta.public) {
      if (to.name === 'login' && user) {
        return '/jobs'
      }
      return true
    }
    if (!user) {
      return {
        name: 'login',
        query: { redirect: to.fullPath },
      }
    }
    return true
  })

  return router
}
