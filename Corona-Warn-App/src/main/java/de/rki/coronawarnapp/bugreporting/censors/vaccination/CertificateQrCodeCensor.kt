package de.rki.coronawarnapp.bugreporting.censors.vaccination

import dagger.Reusable
import de.rki.coronawarnapp.bugreporting.censors.BugCensor
import de.rki.coronawarnapp.bugreporting.censors.BugCensor.CensorContainer
import de.rki.coronawarnapp.covidcertificate.common.certificate.Dcc
import de.rki.coronawarnapp.covidcertificate.common.certificate.DccData
import de.rki.coronawarnapp.covidcertificate.vaccination.core.certificate.VaccinationDccV1
import java.util.LinkedList
import javax.inject.Inject

@Reusable
class CertificateQrCodeCensor @Inject constructor() : BugCensor {

    override suspend fun checkLog(message: String): CensorContainer? {
        var newMessage = CensorContainer(message)

        synchronized(qrCodeStringsToCensor) { qrCodeStringsToCensor.toList() }.forEach {
            newMessage = newMessage.censor(it, PLACEHOLDER + it.takeLast(4))
        }

        synchronized(certsToCensor) { certsToCensor.toList() }.forEach {
            it.certificate.apply {
                val dobFormatted = dateOfBirth.toString()

                newMessage = newMessage.censor(
                    dobFormatted,
                    "vaccinationCertificate/dateOfBirth"
                )
                if (dobFormatted != dob) {
                    newMessage = newMessage.censor(
                        dob,
                        "vaccinationCertificate/dob"
                    )
                }

                newMessage = censorNameData(nameData, newMessage)

                payload.let { data ->
                    newMessage = censorVaccinationData(data, newMessage)
                }
            }
        }

        return newMessage.nullIfEmpty()
    }

    private fun censorVaccinationData(
        vaccinationData: VaccinationDccV1.VaccinationData,
        message: CensorContainer
    ): CensorContainer {
        var newMessage = message

        newMessage = newMessage.censor(
            vaccinationData.marketAuthorizationHolderId,
            "vaccinationData/marketAuthorizationHolderId"
        )

        newMessage = newMessage.censor(
            vaccinationData.medicalProductId,
            "vaccinationData/medicalProductId"
        )

        newMessage = newMessage.censor(
            vaccinationData.targetId,
            "vaccinationData/targetId"
        )

        newMessage = newMessage.censor(
            vaccinationData.certificateIssuer,
            "vaccinationData/certificateIssuer"
        )

        newMessage = newMessage.censor(
            vaccinationData.uniqueCertificateIdentifier,
            "vaccinationData/uniqueCertificateIdentifier"
        )

        newMessage = newMessage.censor(
            vaccinationData.certificateCountry,
            "vaccinationData/certificateCountry"
        )

        newMessage = newMessage.censor(
            vaccinationData.vaccineId,
            "vaccinationData/vaccineId"
        )

        val vaccinatedAt = vaccinationData.vaccinatedAt.toString()
        newMessage = newMessage.censor(
            vaccinatedAt,
            "vaccinationData/vaccinatedAt"
        )
        if (vaccinatedAt != vaccinationData.dt) {
            newMessage = newMessage.censor(
                vaccinationData.dt,
                "vaccinationData/dt"
            )
        }

        return newMessage
    }

    private fun censorNameData(nameData: Dcc.NameData, message: CensorContainer): CensorContainer {
        var newMessage = message

        nameData.familyName?.let { fName ->
            newMessage = newMessage.censor(
                fName,
                "nameData/familyName"
            )
        }

        newMessage = newMessage.censor(
            nameData.familyNameStandardized,
            "nameData/familyNameStandardized"
        )

        nameData.givenName?.let { gName ->
            newMessage = newMessage.censor(
                gName,
                "nameData/givenName"
            )
        }

        nameData.givenNameStandardized?.let { gName ->
            newMessage = newMessage.censor(
                gName,
                "nameData/givenNameStandardized"
            )
        }

        return newMessage
    }

    companion object {
        private val qrCodeStringsToCensor = LinkedList<String>()

        fun addQRCodeStringToCensor(rawString: String) = synchronized(qrCodeStringsToCensor) {
            qrCodeStringsToCensor.apply {
                if (contains(rawString)) return@apply
                addFirst(rawString)
                // Max certs is at 4, but we may scan invalid qr codes that are not added which will be shown in raw
                if (size > 8) removeLast()
            }
        }

        fun clearQRCodeStringToCensor() = synchronized(qrCodeStringsToCensor) { qrCodeStringsToCensor.clear() }

        private val certsToCensor = LinkedList<DccData<VaccinationDccV1>>()
        fun addCertificateToCensor(cert: DccData<VaccinationDccV1>) = synchronized(certsToCensor) {
            certsToCensor.apply {
                if (contains(cert)) return@apply
                addFirst(cert)
                // max certs we should have is 2, 50% leeway
                if (size > 4) removeLast()
            }
        }

        fun clearCertificateToCensor() = synchronized(certsToCensor) { certsToCensor.clear() }

        private const val PLACEHOLDER = "########-####-####-####-########"
    }
}
