/*
 * Copyright 2005-2008 LAMP/EPFL
 */
// $Id$

package scala.tools.eclipse.javaelements

import org.eclipse.core.resources.IFile
import org.eclipse.core.runtime.{ IProgressMonitor, IStatus, Status }
import org.eclipse.jdt.core.{ IJavaElement, IPackageFragment, JavaCore }
import org.eclipse.jdt.internal.core.{ JavaModelManager }
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart
import org.eclipse.ui.progress.UIJob

object JDTUtils {
  private var refreshPending = false
  private val lock = new Object
  
  def refreshPackageExplorer = {
    lock.synchronized{
      if (!refreshPending) {
        refreshPending = true
        new UIJob("Refresh package explorer") {
          def runInUIThread(monitor : IProgressMonitor) : IStatus  = {
            lock.synchronized {
              refreshPending = false
            }
            val pep = PackageExplorerPart.getFromActivePerspective
            if (pep != null)
              pep.getTreeViewer.refresh()
            Status.OK_STATUS
          }
        }.schedule
      }
    }
  }

  def getParentPackage(scalaFile : IFile) : IPackageFragment = {
    val jp = JavaCore.create(scalaFile.getProject)
    val pkg = JavaModelManager.determineIfOnClasspath(scalaFile, jp)
    if (pkg != null && pkg.isInstanceOf[IPackageFragment])
      pkg.asInstanceOf[IPackageFragment]
    else {
      // Not on classpath so use the default package
      val root = jp.getPackageFragmentRoot(scalaFile.getParent)
      root.getPackageFragment(IPackageFragment.DEFAULT_PACKAGE_NAME)
    }
  }
}

object JavaElementInfoUtils extends ReflectionUtils {
  private val clazz = Class.forName("org.eclipse.jdt.internal.core.JavaElementInfo")
  private val addChildMethod = getMethod(clazz, "addChild", classOf[IJavaElement])
  private val getChildrenMethod = getMethod(clazz, "getChildren")
  private val removeChildMethod = getMethod(clazz, "removeChild", classOf[IJavaElement])
  private val setChildrenMethod = getMethod(clazz, "setChildren", classOf[Array[IJavaElement]])
  
  def addChild(info : AnyRef, child : IJavaElement) = addChildMethod.invoke(info, child)
  def getChildren(info : AnyRef) : Array[IJavaElement] = getChildrenMethod.invoke(info).asInstanceOf[Array[IJavaElement]]
  def removeChild(info : AnyRef, child : IJavaElement) = removeChildMethod.invoke(info, child)
  def setChildren(info : AnyRef, children : Array[IJavaElement]) = setChildrenMethod.invoke(info, children : AnyRef)
}

object SourceRefElementInfoUtils extends ReflectionUtils {
  import java.lang.Integer

  private val sreiClazz = Class.forName("org.eclipse.jdt.internal.core.SourceRefElementInfo")
  private val setSourceRangeStartMethod = getMethod(sreiClazz, "setSourceRangeStart", classOf[Int])
  private val setSourceRangeEndMethod = getMethod(sreiClazz, "setSourceRangeEnd", classOf[Int])
  
  def setSourceRangeStart(start : Int) : Unit = setSourceRangeStartMethod.invoke(this, new Integer(start))
  def setSourceRangeEnd(end : Int) : Unit = setSourceRangeEndMethod.invoke(this, new Integer(end))
}