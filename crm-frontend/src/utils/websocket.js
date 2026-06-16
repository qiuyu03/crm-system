import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

let stompClient = null

export function connectWebSocket(onAlertCallback, onStatusCallback) {
  stompClient = new Client({
    webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
    reconnectDelay: 5000,
    onConnect: () => {
      console.log('WebSocket 已连接')

      // 订阅全局预警
      stompClient.subscribe('/topic/alerts', (message) => {
        const alert = JSON.parse(message.body)
        onAlertCallback && onAlertCallback(alert)
      })
    },
    onDisconnect: () => console.log('WebSocket 已断开')
  })
  stompClient.activate()
}

// 订阅单个订单的状态变更
export function subscribeOrderStatus(orderId, callback) {
  if (!stompClient?.connected) return
  stompClient.subscribe(`/topic/orders/${orderId}/status`, (message) => {
    callback(JSON.parse(message.body))
  })
}

export function disconnect() {
  stompClient?.deactivate()
}
