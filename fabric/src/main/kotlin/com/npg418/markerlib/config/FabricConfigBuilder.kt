package com.npg418.markerlib.config

import com.mojang.logging.LogUtils
import com.npg418.markerlib.config.api.*
import kotlinx.serialization.json.*
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.Logger
import java.nio.file.Files
import java.nio.file.Path
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.safeCast


class FabricConfigBuilder(private val spec: ConfigSpec) : ConfigTreeWalker(LOGGER), ConfigPropertyVisitor<Unit> {
    companion object {
        private val LOGGER: Logger = LogUtils.getLogger()
        private val JSON = Json { prettyPrint = true }
    }

    private var existing = JsonObject(emptyMap())
    private var children = mutableMapOf<String, JsonElement>()

    fun register(modId: String) {
        val path = FabricLoader.getInstance().configDir.resolve("$modId-${spec.configType.name.lowercase()}.json")
        existing = readJson(path)
        children = mutableMapOf()
        walkRoot(spec::class).fold(onSuccess = {
            writeJson(path, JsonObject(children))
        }, onFailure = {
            LOGGER.error("An error occurred while registering {}.", spec::class.simpleName, it)
        })
    }

    override fun onProperty(name: String, property: KProperty1<out Any, *>, delegate: ConfigProperty<*>) {
        delegate.accept(name, this)
    }

    override fun onSection(name: String, sectionClass: KClass<*>, visitChildren: () -> Unit) {
        val parentExisting = existing
        val parentChildren = children
        existing = existing[name] as? JsonObject ?: JsonObject(emptyMap())
        children = mutableMapOf()

        visitChildren()

        parentChildren[name] = JsonObject(children)
        existing = parentExisting
        children = parentChildren
    }

    private fun primitiveOf(name: String): JsonPrimitive? =
        (existing[name] as? JsonPrimitive)?.takeIf { it !is JsonNull }

    private fun <T : Any> emit(name: String, p: ConfigProperty<T>) {
        children[name] = toJsonPrimitive(p.getter())
    }

    override fun <T : Any> visit(name: String, p: NormalConfigProperty<T>) {
        primitiveOf(name)?.let { p.setter(parse(name, p.type, p.default, it)) }
        emit(name, p)
    }

    override fun <T : Comparable<T>> visit(name: String, p: RangedConfigProperty<T>) {
        primitiveOf(name)?.let {
            val parsed = parse(name, p.type, p.default, it)
            val clamped = parsed.coerceIn(p.range)
            if (parsed != clamped) {
                LOGGER.warn(
                    "Config entry {} (value: {}) is out of range {}. Clamped to {}.", name, parsed, p.range, clamped
                )
            }
            p.setter(clamped)
        }
        emit(name, p)
    }

    override fun <T : Any> visit(name: String, p: OneOfConfigProperty<T>) {
        primitiveOf(name)?.let { p.setter(parse(name, p.type, p.default, it)) }
        emit(name, p)
    }

    override fun <E : Enum<E>> visit(name: String, p: EnumConfigProperty<E>) {
        primitiveOf(name)?.let {
            val enumValues = p.type.java.enumConstants
            val found = enumValues.firstOrNull { e -> e.name.equals(it.content, ignoreCase = true) }
            if (found == null) {
                LOGGER.warn(
                    "Enum config entry {} (value: {}) is not one of [{}]. Falling back to default one ({}).",
                    name,
                    it.content,
                    enumValues.joinToString { e -> e.name },
                    p.default
                )
            } else p.setter(found)
        }
        emit(name, p)
    }

    override fun <E : Any> visit(name: String, p: ListConfigProperty<E>) {
        val array = existing[name] as? JsonArray
        if (array != null) bindList(name, p, array)
        children[name] = JsonArray(p.getter().map(::toJsonPrimitive))
    }

    private fun <E : Any> bindList(name: String, p: ListConfigProperty<E>, array: JsonArray) {
        if (array.isEmpty() && !p.allowEmpty) {
            LOGGER.warn("Config entry {} must not be empty. Falling back to default one.", name)
            return
        }
        val sample = p.newElement()
        val result = array.mapIndexedNotNull { i, elem ->
            val primitive = elem as? JsonPrimitive
            if (primitive == null) {
                LOGGER.warn("Config entry {}[{}] is not primitive value. Skipping.", name, i)
                return@mapIndexedNotNull null
            }
            val parsed = parse("$name[$i]", p.elementType, sample, primitive)
            if (!p.elementValidator(parsed)) {
                LOGGER.warn("Config entry {}[{}] (value: {}) failed validation. Skipping.", name, i, parsed)
                return@mapIndexedNotNull null
            }
            parsed
        }
        if (result.isEmpty() && !p.allowEmpty) {
            LOGGER.warn("Config entry {} became empty after validation. Falling back to default one.", name)
            return
        }
        p.setter(result)
    }

    private fun <T : Any> parse(name: String, type: KClass<T>, default: T, primitive: JsonPrimitive): T {
        val parsed: Any? = when (type) {
            Boolean::class -> primitive.booleanOrNull
            Int::class -> primitive.intOrNull
            Long::class -> primitive.longOrNull
            Double::class -> primitive.doubleOrNull
            Float::class -> primitive.floatOrNull
            String::class -> primitive.contentOrNull
            else -> throw IllegalArgumentException("Unsupported config value type: ${type.qualifiedName}")
        }
        return type.safeCast(parsed) ?: run {
            LOGGER.warn(
                "Config entry {} (value: {}) is an invalid representation. Falling back to default one ({}).",
                name,
                primitive,
                default
            )
            default
        }
    }

    private fun toJsonPrimitive(value: Any): JsonPrimitive = when (value) {
        is Number -> JsonPrimitive(value)
        is Boolean -> JsonPrimitive(value)
        is String -> JsonPrimitive(value)
        is Enum<*> -> JsonPrimitive(value.name)
        else -> throw IllegalArgumentException("Unsupported config value type: ${value::class.simpleName}")
    }

    private fun readJson(path: Path): JsonObject = runCatching {
        JSON.parseToJsonElement(Files.readString(path)).jsonObject
    }.getOrElse {
        LOGGER.warn("Config file {} was not found or invalid. Falling back default one.", path.fileName)
        JsonObject(emptyMap())
    }

    private fun writeJson(path: Path, jsonObject: JsonObject) {
        runCatching {
            Files.createDirectories(path.parent)
            Files.writeString(path, JSON.encodeToString(JsonObject.serializer(), jsonObject))
        }.onFailure {
            LOGGER.error("Unable to write config file {}.", path.fileName)
        }
    }
}