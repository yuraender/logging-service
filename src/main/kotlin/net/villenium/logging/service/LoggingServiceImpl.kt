package net.villenium.logging.service

import com.google.protobuf.Empty
import io.grpc.stub.StreamObserver
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Category
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.TextChannel
import net.villenium.grpc.stream.Streams
import net.villenium.logging.LoggingServiceGrpc.LoggingServiceImplBase
import net.villenium.logging.LoggingServiceOuterClass
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*
import kotlin.jvm.optionals.getOrElse

class LoggingServiceImpl(
    private val jda: JDA,
    private val guildId: Long,
    private val categoryId: Long
) : LoggingServiceImplBase() {

    override fun logMessage(
        request: LoggingServiceOuterClass.MessageRequest,
        responseObserver: StreamObserver<Empty>
    ) {
        val guild: Guild = jda.getGuildById(guildId)
            ?: return
        val category: Category = guild.getCategoryById(categoryId)
            ?: return

        val channel: TextChannel = category.textChannels
            .stream()
            .filter { it.name.equals(request.channel, true) }
            .findAny()
            .getOrElse {
                val action = category.createTextChannel(request.channel)
                for (it in request.rolesList) {
                    guild.getRoleById(it)
                        ?: continue
                    action.addRolePermissionOverride(it, listOf(
                        Permission.VIEW_CHANNEL,
                        Permission.MESSAGE_READ,
                        Permission.MESSAGE_HISTORY,
                    ), listOf(
                        Permission.MESSAGE_WRITE
                    ))
                }
                action.complete()
            }

        val time: String = SimpleDateFormat("HH:mm:ss").format(Date.from(Instant.now()))
        channel.sendMessage(
            "`%s` ${request.message}"
                .replace('\'', '`')
                .format(time)
        ).queue()

        Streams.write(responseObserver, EMPTY)
    }

    companion object {
        private val EMPTY = Empty.newBuilder().build()
    }
}
