package de.rki.coronawarnapp.eventregistration.checkins.qrcode

import de.rki.coronawarnapp.environment.EnvironmentSetup
import de.rki.coronawarnapp.eventregistration.common.decodeBase32
import de.rki.coronawarnapp.server.protocols.internal.pt.TraceLocationOuterClass
import de.rki.coronawarnapp.util.security.SignatureValidation
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runBlockingTest
import org.joda.time.Instant
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import testhelpers.BaseTestInstrumentation

@RunWith(JUnit4::class)
@Ignore("FIXME: Provide new encoded signed trace location samples")
class DefaultQRCodeVerifierTest : BaseTestInstrumentation() {

    @MockK lateinit var environmentSetup: EnvironmentSetup
    private lateinit var qrCodeVerifier: QRCodeVerifier

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        every { environmentSetup.appConfigVerificationKey } returns PUB_KEY
        qrCodeVerifier = DefaultQRCodeVerifier(SignatureValidation(environmentSetup))
    }

    @Test
    fun verifyEventSuccess() = runBlockingTest {
        val time = 2687960 * 1_000L
        val instant = Instant.ofEpochMilli(time)
        shouldNotThrowAny {
            val verifyResult = qrCodeVerifier.verify(ENCODED_EVENT)
            verifyResult.apply {
                singedTraceLocation.location.description shouldBe "CWA Launch Party"
                verifyResult.isBeforeStartTime(instant) shouldBe false
                verifyResult.isAfterEndTime(instant) shouldBe false
            }
        }
    }

    @Test
    fun verifyEventStartTimeWaning() = runBlockingTest {
        val time = 2687940 * 1_000L
        val instant = Instant.ofEpochMilli(time)
        shouldNotThrowAny {
            val verifyResult = qrCodeVerifier.verify(ENCODED_EVENT)
            verifyResult.apply {
                singedTraceLocation.location.description shouldBe "CWA Launch Party"
            }
            verifyResult.isBeforeStartTime(instant) shouldBe true
            verifyResult.isAfterEndTime(instant) shouldBe false
        }
    }

    @Test
    fun verifyEventEndTimeWarning() = runBlockingTest {
        val instant = Instant.now()
        shouldNotThrowAny {
            val verifyResult = qrCodeVerifier.verify(ENCODED_EVENT)
            verifyResult.apply {
                singedTraceLocation.location.description shouldBe "CWA Launch Party"
            }
            verifyResult.isBeforeStartTime(instant) shouldBe false
            verifyResult.isAfterEndTime(instant) shouldBe true
        }
    }

    @Test
    fun verifyEventWithInvalidKey() = runBlockingTest {
        every { environmentSetup.appConfigVerificationKey } returns INVALID_PUB_KEY
        shouldThrow<InvalidQRCodeSignatureException> {
            qrCodeVerifier.verify(ENCODED_EVENT)
        }
    }

    @Test
    fun eventHasMalformedData() = runBlockingTest {
        shouldThrow<InvalidQRCodeDataException> {
            qrCodeVerifier.verify(INVALID_ENCODED_EVENT)
        }
    }

    companion object {

        private const val INVALID_PUB_KEY = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEc7DEstcUIRcyk35OYDJ95/hTg" +
            "3UVhsaDXKT0zK7NhHPXoyzipEnOp3GyNXDVpaPi3cAfQmxeuFMZAIX2+6A5Xg=="

        private const val PUB_KEY = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEafIKZOiRPuJWjKOUmKv7OTJWTyii" +
            "4oCQLcGn3FgYoLQaJIvAM3Pl7anFDPPY/jxfqqrLyGc0f6hWQ9JPR3QjBw=="

        private const val INVALID_ENCODED_EVENT =
            "BIPEY33SMVWSA2LQON2W2IDEN5WG64RAONUXIIDBNVSXILBAMNXRBCM4UQARRKM6UQASAHRKCC7CTDWGQ" +
                "4JCO7RVZSWVIMQK4UPA.GBCAEIA7TEORBTUA25QHBOCWT26BCA5PORBS2E4FFWMJ3U" +
                "U3P6SXOL7SHUBCA7UEZBDDQ2R6VRJH7WBJKVF7GZYJA6YMRN27IPEP7NKGGJSWX3XQ"

        private const val ENCODED_EVENT =
            "BIYAUEDBZY6EIWF7QX6JOKSRPAGEB3H7CIIEGV2BEBGGC5LOMNUCAUDBOJ2" +
                "HSGGTQ6SACIHXQ6SACKA6CJEDARQCEEAPHGEZ5JI2K2T422L5U3SMZY5DGC" +
                "PUZ2RQACAYEJ3HQYMAFFBU2SQCEEAJAUCJSQJ7WDM675MCMOD3L2UL7ECJU" +
                "7TYERH23B746RQTABO3CTI="
    }
}