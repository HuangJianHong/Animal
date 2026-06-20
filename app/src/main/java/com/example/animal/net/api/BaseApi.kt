package com.example.animal.net.api

/**
 * 业务 Api 接口基类（标记接口）。
 *
 * 所有业务 Api 接口建议继承本接口，便于：
 * 1. 统一约束与识别项目内的网络接口；
 * 2. 后续可在此扩展公共方法（如统一心跳、配置拉取等）。
 *
 * 注意：Retrofit 接口本身不能有方法实现，公共能力通过组合/扩展实现，这里仅作类型规范。
 */
interface BaseApi
