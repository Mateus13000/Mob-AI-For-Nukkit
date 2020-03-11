package br.com.gamemods.mobai.entity.smart.logic

import br.com.gamemods.mobai.delegators.transforming
import br.com.gamemods.mobai.entity.smart.EntityAI
import br.com.gamemods.mobai.entity.smart.EntityProperties
import br.com.gamemods.mobai.entity.smart.SmartEntity
import cn.nukkit.entity.Attribute
import cn.nukkit.entity.Entity
import cn.nukkit.entity.EntityType
import cn.nukkit.entity.impl.BaseEntity
import cn.nukkit.level.Level
import cn.nukkit.math.Vector3f
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.reflect.KProperty

interface SplitLogic: EntityProperties {
    val ai: EntityAI<*>

    fun setMaxHealth(maxHealth: Int) {
        this.maxHealth = maxHealth.toFloat()
    }

    fun updateAttribute(id: Int): Attribute?

    fun randomParticlePos(
        xScale: Double = 1.0,
        yScale: Double = random.nextDouble(),
        zScale: Double = 1.0,
        xOffset: Double = 0.0,
        yOffset: Double = 0.0,
        zOffset: Double = 0.0
    ): Vector3f {
        base {
            val random = random
            val width = width
            return Vector3f(
                x + xOffset + width * (2.0 * random.nextDouble() - 1.0) * xScale,
                y + yOffset + height * yScale,
                z + zOffset + width * (2.0 * random.nextDouble() - 1.0) * zScale
            )
        }
    }
}

inline val SplitLogic.entity get() = this as Entity
inline val SplitLogic.base get() = this as BaseEntity
inline val SplitLogic.smart get() = this as SmartEntity

inline val SplitLogic.level: Level get() = base.level
inline val SplitLogic.type: EntityType<*> get() = entity.type

private val baseEntityHealthField = BaseEntity::class.java.getDeclaredField("maxHealth").also { it.isAccessible = true }
private var BaseEntity.maxHealthField: Int
    get() = baseEntityHealthField.getInt(this)
    set(value) {
        baseEntityHealthField.setInt(this, value)
    }

var SplitLogic.maxHealth by transforming(20F) { thisRef: SplitLogic, _: KProperty<*>, _: Float, newValue: Float ->
    thisRef.smart {
        ifNotOnInit {
            val updated = recalculateAttribute(maxHealthAttribute)
            if (updated != newValue) {
                base.maxHealthField = updated.toInt()
                return@transforming updated
            }
        }
        base.maxHealthField = newValue.toInt()
    }
    newValue
}

@ExperimentalContracts
inline fun SplitLogic.smart(operation: SmartEntity.() -> Unit) {
    contract {
        callsInPlace(operation, InvocationKind.EXACTLY_ONCE)
    }
    smart.apply(operation)
}

@ExperimentalContracts
inline fun SplitLogic.base(operation: BaseEntity.() -> Unit) {
    contract {
        callsInPlace(operation, InvocationKind.EXACTLY_ONCE)
    }
    base.apply(operation)
}

inline fun SplitLogic.entity(operation: Entity.() -> Unit) {
    contract {
        callsInPlace(operation, InvocationKind.EXACTLY_ONCE)
    }
    entity.apply(operation)
}

inline fun <R> SplitLogic.runSmart(operation: SmartEntity.() -> R) = smart.run(operation)
inline fun <R> SplitLogic.runBase(operation: BaseEntity.() -> R) = base.run(operation)
inline fun <R> SplitLogic.runEntity(operation: Entity.() -> R) = entity.run(operation)


// The way Nukkit designed entities makes this get called before this object is fully setup,
// causing NPE on instantiation
inline fun SplitLogic.ifNotOnInit(action: () -> Unit) {
    try {
        definitions.hashCode()
    } catch (_: NullPointerException) {
        return
    }

    action()
}
