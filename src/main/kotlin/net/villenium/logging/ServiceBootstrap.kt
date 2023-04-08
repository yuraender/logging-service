package net.villenium.logging

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.ChunkingFilter
import net.dv8tion.jda.api.utils.MemberCachePolicy
import net.dv8tion.jda.api.utils.cache.CacheFlag
import net.villenium.grpc.authorization.service.TokenService
import net.villenium.grpc.server.GrpcServer
import net.villenium.logging.service.LoggingServiceImpl

class ServiceBootstrap {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val jda: JDA = JDABuilder
                .createDefault(
                    System.getenv("TOKEN"),
                    listOf(GatewayIntent.GUILD_MESSAGES)
                )
                .setStatus(OnlineStatus.valueOf(System.getenv("STATUS")))
                .disableCache(CacheFlag.EMOTE, CacheFlag.VOICE_STATE)
                .setMemberCachePolicy(MemberCachePolicy.NONE)
                .setChunkingFilter(ChunkingFilter.NONE)
                .build().awaitReady()
            val guildId: Long = System.getenv("GUILD").toLong()
            val categoryId: Long = System.getenv("CATEGORY").toLong()

            val tokenService = TokenService()
            val server: GrpcServer = GrpcServer.bindWithBaseAuthorization(
                System.getenv("PORT").toInt(),
                LoggingServiceImpl(jda, guildId, categoryId)
            ) {
                tokenService.findAccount(it.login, it.token).orElse(null).hasPermission("logging-service")
            }
            server.awaitTermination()
        }
    }
}
