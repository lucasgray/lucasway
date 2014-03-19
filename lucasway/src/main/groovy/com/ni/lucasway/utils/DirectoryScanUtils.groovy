package com.ni.lucasway.utils

import groovy.io.FileType

public class DirectoryScanUtils
{
	def static stripExtension(filePath)
	{
		def lastSlashAt = filePath.lastIndexOf('/')
		def fileDirPath = filePath.substring(0, lastSlashAt)
		def fileName = filePath.substring(filePath.lastIndexOf('/') + 1)
		return "${fileDirPath}/${fileName.substring(0, fileName.indexOf('.'))}".toString()
	}

	def static getRelativePath(parentDir, dir)
	{
		def parentDirPath = parentDir.getAbsolutePath()
		def dirPath = dir.getAbsolutePath()
		if (dirPath.startsWith(parentDirPath)) {
			return dirPath.substring(parentDirPath.length(), dirPath.length())
		}
		else {
			return dirPath
		}
	}

	def static ensureFileTyped(fileRef) {
		return fileRef instanceof File ? fileRef : new File(fileRef)
	}

	def static buildAndProcessNodes(File dir, ObjectNode parentNode = null, consumeDir) {
		def node = new ObjectNode(parentNode, dir.name, consumeDir(dir, parentNode))
		dir.eachFile(FileType.DIRECTORIES) { subDir ->
			if (! subDir.name.startsWith('.')) {
				buildAndProcessNodes(subDir, node, consumeDir)
			}
		}
		return node
	}
}