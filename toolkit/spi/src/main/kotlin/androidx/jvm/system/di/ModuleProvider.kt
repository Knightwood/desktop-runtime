package androidx.jvm.system.di

import org.koin.core.module.Module

interface ModuleProvider {
    fun provide(): Module
}
