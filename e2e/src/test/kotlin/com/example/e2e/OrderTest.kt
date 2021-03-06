package com.example.e2e

import com.example.e2e.container.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.testcontainers.containers.Network
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
class OrderTest {

    private val network = Network.newNetwork()

    @Container
    val eurekaContainer = EurekaContainer(network)

    @Container
    val configContainer = ConfigContainer(network, eurekaContainer)

    @Container
    val zookeeperContainer = ZookeeperContainer(network)

    @Container
    val kafkaContainer = KafkaContainer(network, zookeeperContainer)

    @Container
    val postgresContainer = PostgresContainer(network)

    @Container
    val cdcContainer = CdcContainer(network,
            zookeeperContainer = zookeeperContainer,
            kafkaContainer = kafkaContainer,
            postgresContainer = postgresContainer)

    @Container
    val storeContainer = StoreContainer(network,
            cdcContainer = cdcContainer,
            eurekaContainer = eurekaContainer,
            configContainer = configContainer)

    @Container
    val orderContainer = OrderContainer(network,
            cdcContainer = cdcContainer,
            eurekaContainer = eurekaContainer,
            configContainer = configContainer)

    @Container
    val deliveryContainer = DeliveryContainer(network,
            cdcContainer = cdcContainer,
            eurekaContainer = eurekaContainer,
            configContainer = configContainer)

    private val restExecutor = RestExecutor()

    @Test
    fun test() {
        assertTrue(storeContainer.isRunning)
        assertTrue(orderContainer.isRunning)
        assertTrue(deliveryContainer.isRunning)
        val url = "http://${orderContainer.host}:${orderContainer.firstMappedPort}"
        val order = restExecutor.createOrder(url)
        val orderBody = order.body!!
        assertEquals(HttpStatus.OK, order.statusCode)
        assertNotNull(orderBody)
        val orderById = restExecutor.getOrderById(url, orderBody.id.toString())
        assertEquals(HttpStatus.OK, orderById.statusCode)
        assertNotNull(orderById.body)
    }
}