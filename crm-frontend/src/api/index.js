import http from './http'

export const customerApi = {
  list: (params) => http.get('/api/customers', { params }),
  get: (id) => http.get(`/api/customers/${id}`),
  profile: (id) => http.get(`/api/customers/${id}/profile`),
  create: (data) => http.post('/api/customers', data),
  rescore: (id) => http.put(`/api/customers/${id}/rescore`)
}

export const orderApi = {
  list: (params) => http.get('/api/orders', { params }),
  detail: (id) => http.get(`/api/orders/${id}`),
  create: (data) => http.post('/api/orders', data),
  changeStatus: (id, status) => http.put(`/api/orders/${id}/status`, null, { params: { status } }),
  triggerAlert: (id, alertType, message) =>
    http.post(`/api/orders/${id}/alert`, null, { params: { alertType, message } })
}
