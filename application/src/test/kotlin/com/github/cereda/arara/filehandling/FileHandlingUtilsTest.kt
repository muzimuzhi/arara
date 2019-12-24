package com.github.cereda.arara.filehandling

import com.github.cereda.arara.Arara
import com.github.cereda.arara.configuration.AraraSpec
import com.github.cereda.arara.model.AraraException
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.ShouldSpec
import java.io.File
import java.nio.file.Files

class FileHandlingUtilsTest : ShouldSpec({
    should("fail generating CRC sums on inexistent files") {
        shouldThrow<AraraException> {
            FileHandlingUtils.calculateHash(File("QUACK"))
        }
    }
    should("generate correct CRC sums") {
        FileHandlingUtils.calculateHash(File("../LICENSE")) shouldBe "17f430a5"
        FileHandlingUtils.calculateHash(File("../CODE_OF_CONDUCT.md")) shouldBe "536c426f"
    }

    should("find correct extension") {
        FileHandlingUtils.getFileExtension(File("QUACK")) shouldBe ""
        FileHandlingUtils.getFileExtension(File("a.tex")) shouldBe "tex"
        FileHandlingUtils.getFileExtension(File(".tex")) shouldBe "tex"
    }
    should("find correct basename") {
        FileHandlingUtils.getBasename(File("QUACK")) shouldBe "QUACK"
        FileHandlingUtils.getBasename(File("a.tex")) shouldBe "a"
        FileHandlingUtils.getBasename(File(".tex")) shouldBe ""
    }
    should("find correct full basename") {
        val currentPath = File("").absolutePath
        FileHandlingUtils.getFullBasename(File("a/QUACK")) shouldBe "$currentPath/a/QUACK"
        FileHandlingUtils.getFullBasename(File("a.tex")) shouldBe "a"
        FileHandlingUtils.getFullBasename(File("meow/a/.tex")) shouldBe "$currentPath/meow/a"
    }

    should("get subdirecotry relationship right") {
        FileHandlingUtils.isSubDirectory(File("../docs"), File("..")) shouldBe true
        FileHandlingUtils.isSubDirectory(File(".."), File("../docs")) shouldBe false
        shouldThrow<AraraException> {
            FileHandlingUtils.isSubDirectory(File("../LICENSE"), File(".."))
        }
        shouldThrow<AraraException> {
            FileHandlingUtils.isSubDirectory(File(".."), File("../LICENSE"))
        }
    }

    should("detect changes on file") {
        val file = Files.createTempFile(null, null).toFile()
        val referenceBackup = Arara.config[AraraSpec.Execution.reference]
        Arara.config[AraraSpec.Execution.reference] = file.parentFile.resolve("reference")
        FileHandlingUtils.hasChanged(file) shouldBe true
        FileHandlingUtils.hasChanged(file) shouldBe false
        file.writeText("QUACK")
        FileHandlingUtils.hasChanged(file) shouldBe true
        FileHandlingUtils.hasChanged(file) shouldBe false
        file.writeText("QUACK2")
        FileHandlingUtils.hasChanged(file) shouldBe true
        file.delete()
        FileHandlingUtils.hasChanged(file) shouldBe true
        FileHandlingUtils.hasChanged(file) shouldBe false
        Arara.config[AraraSpec.Execution.reference] = referenceBackup
    }
})
