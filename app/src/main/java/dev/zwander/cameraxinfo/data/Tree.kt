package dev.zwander.cameraxinfo.data

import android.content.Context
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import java.io.File

data class Node(
    val name: String = "",
    val parent: (() -> Node?) = { null },
    val children: MutableList<Node> = mutableListOf(),
    val content: String? = null
) {
    val absolutePath: String
        get() = (parent()?.absolutePath ?: "") + name + if (content == null) "/" else ""

    val root: Node
        get() = parent()?.root ?: this
}

fun QuerySnapshot.createZipFile(context: Context): File {
    val dataRoot = File(context.cacheDir, "data")
    if (dataRoot.exists()) {
        dataRoot.deleteRecursively()
    }

    val file = ZipFile(File(context.cacheDir, "data.zip"))

    if (file.file.exists()) {
        file.file.delete()
    }

    forEach { doc ->
        val path = doc.reference.path.replace("CameraDataNode/", "")
        val docFile = File(dataRoot, "${path}.json")
        docFile.parentFile?.mkdirs()

        docFile.writeBytes(doc.data.values.last().toString().toByteArray())

        file.addFile(docFile, ZipParameters().apply {
            this.fileNameInZip = "${path}.json"
        })
    }

    file.close()

    return file.file
}

fun QuerySnapshot.createTreeFromPaths(): Node {
    val root = Node()

    forEach { doc ->
        val split = doc.reference.path.split("/")

        handleNode(root, split, doc)
    }

    return root
}

private fun handleNode(parent: Node, split: List<String>, doc: QueryDocumentSnapshot) {
    if (split.isEmpty()) {
        return
    }

    val first = split.first()

    var nextList = split.drop(1)
    if (nextList.firstOrNull() == "CameraDataNode") {
        nextList = nextList.drop(1)
    }
    val item = parent.children.find { it.name == first }

    if (item == null) {
        val node = Node(
            name = first,
            parent = { parent },
            content = if (nextList.isEmpty()) doc.data.values.last()?.toString() else null
        )

        parent.children.add(node)

        handleNode(node, nextList, doc)
    } else {
        handleNode(item, nextList, doc)
    }
}