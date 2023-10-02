package test.paragraph2.kdoc

/**
 * @property name property info
 */
class A constructor(
    val name: String
) {}

/**
 * @property name property info
 */
class A constructor(
    //single-line comment
    val name: String
) {}

/**
 * @property name property info
 */
class A constructor(
    /*
     * block
     * comment
     */
    val name: String
) {}

/**
 * @property name property info
 */
class A constructor(
    /**
     * kdoc property
     * comment
     */
    val name: String
) {}

/**
 * @property name property info
 */
class A constructor(
    /**
     * @property name property
     * comment
     */
    val name: String
) {}

/**
 * @property openName open property info
 * @property openLastName
 *     open last property
 *     info
 * @property openAddr
 *     property
 *     info
 */
open class B constructor(
    //single-line comment
    open val openName: String,
    /*
     * block
     * comment
     */
    open val openLastName: String,
    /**
     * kdoc property
     * comment
     */
    open val openBirthDate: String,
    /**
     * @property openAddr property
     * comment
     */
    open val openAddr: String
) {}

/**
 * @property internalName internal
 *     property info
 * @property openName override
 *     property info
 * @property privateLastName private
 *     property info
 * @property openAddr override
 *     property info
 */
class A constructor(
    // single-line comment
    private val privateName: String,
    // single-line comment
    protected val protectedName: String,
    // single-line comment
    internal val internalName: String,
    // single-line comment
    override val openName: String,
    // single-line comment
    val name: String,
    /*
     * block
     * comment
     */
    private val privateLastName: String,
    /*
     * block
     * comment
     */
    protected val protectedLastName: String,
    /*
     * block
     * comment
     */
    internal val internalLastName: String,
    /*
     * block
     * comment
     */
    override val openLastName: String,
    /*
     * block
     * comment
     */
    val lastName: String,
    /**
     * kdoc property
     * comment
     */
    private val privateBirthDate: String,
    /**
     * kdoc property
     * comment
     */
    protected val protectedBirthDate: String,
    /**
     * kdoc property
     * comment
     */
    internal val internalBirthDate: String,
    /**
     * kdoc property
     * comment
     */
    override val openBirthDate: String,
    /**
     * kdoc property
     * comment
     */
    val birthDate: String,
    /**
     * @property privateAddr property
     * comment
     */
    private val privateAddr: String,
    /**
     * @property protectedAddr property
     * comment
     */
    protected val protectedAddr: String,
    /**
     * @property internalAddr property
     * comment
     */
    internal val internalAddr: String,
    /**
     * @property openAddr property
     * comment
     */
    override val openAddr: String,
    /**
     * @property addr property
     * comment
     */
    val addr: String,
) : B() {}
