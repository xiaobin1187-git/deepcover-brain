/*
 * Copyright 2024-2026 DeepCover
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.deepcover.brain.service.service;

import io.deepcover.brain.dal.complexitymapper.MethodComplexityMapper;
import io.deepcover.brain.dal.entity.MethodComplexity;
import io.deepcover.brain.dal.entity.SceneRiskLevel;
import io.deepcover.brain.dal.mapper.SceneRiskLevelMapper;
import io.deepcover.brain.model.RiskEnum;
import io.deepcover.brain.model.RiskLevelVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 场景风险等级服务
 *
 * <p>该服务负责评估和管理系统中各个场景的风险等级，基于多维度的风险指标进行综合评估。主要功能包括：</p>
 * <ul>
 *   <li>基于场景流量的风险评估，支持高、中、低三级风险分类</li>
 *   <li>基于方法圈复杂度的风险分析，评估代码维护难度</li>
 *   <li>核心场景的自动识别和高风险标记</li>
 *   <li>支持风险等级的加权计算和综合决策</li>
 *   <li>提供可配置的风险阈值参数管理</li>
 *   <li>生成服务级别的风险评估规则说明</li>
 *   <li>为代码变更和发布决策提供风险参考</li>
 * </ul>
 *
 * <p>该服务通过量化分析场景流量和代码复杂度，为系统的稳定性保障提供科学的决策依据。
 * 帮助开发团队识别高风险变更，合理分配测试资源，确保系统变更的安全性。</p>
 *
 * @author system
 * @version 1.0
 * @since 2023-11-21
 */
@Slf4j
@Service
public class SceneRiskLevelService {

    @Autowired
    MethodComplexityMapper complexityMapper;

    @Autowired
    SceneRiskLevelMapper sceneRiskLevelMapper;

    //圈复杂度低位置
    @Value("${cxty.lower.quantile}")
    int cxtyLowerQuantile;
    //圈复杂度高位置
    @Value("${cxty.higher.quantile}")
    int cxtyHigherQuantile;

    /**
     * 获取方法修改的风险等级
     *
     * <p>根据应用、方法名、流量和核心场景标识，综合评估方法修改的风险等级。该方法负责：</p>
     * <ul>
     *   <li>判断是否为核心场景，核心场景直接定义为高风险</li>
     *   <li>基于场景流量进行风险评估，判断业务影响程度</li>
     *   <li>基于方法圈复杂度进行技术风险评估，评估维护难度</li>
     *   <li>综合场景风险和技术风险，选择更高风险等级作为最终评估</li>
     *   <li>生成详细的决策依据和量化指标</li>
     * </ul>
     *
     * <p>风险评估结果用于代码审查、发布决策和测试资源分配。</p>
     *
     * @param app 应用名称，指定要评估的应用系统
     * @param methodName 方法名称，指定要评估的具体方法
     * @param flow 场景流量，表示该方法被调用的频率
     * @param isCore 核心场景标识，0表示核心场景，1表示非核心场景
     * @return RiskLevelVO 返回风险等级评估结果，包含风险等级、流量数据和决策依据
     */
    public RiskLevelVO getModifyMethodRisk(String app, String methodName, int flow, int isCore) {
        RiskLevelVO riskLevelVO = new RiskLevelVO();
        riskLevelVO.setFlowNum(flow);
        String coreName = "核心场景", sceneFlow = "场景流量", complexity = "圈复杂度";
        //如果是核心场景，则定义为高风险
        if (0 == isCore) {
            riskLevelVO.setDecision(coreName);
            riskLevelVO.setRiskEnum(RiskEnum.HIGHRISK);
            return riskLevelVO;
        }
        //获取场景风险
        RiskEnum sceneRisk = getSceneRisk(app, flow);
        //获取圈复杂度风险
        RiskLevelVO complexityRisk = getComplexityRisk(app, methodName);
        if (sceneRisk.getCode() <= complexityRisk.getRiskEnum().getCode()) {
            //场景风险高于圈复杂度风险
            if (5 > sceneRisk.getCode()) {
                //不是未知风险才设置
                riskLevelVO.setDecision(sceneFlow + ":" + flow + ";" + complexity + ":" + complexityRisk.getComplexityValue());
            }
            riskLevelVO.setFlowNum(flow);
            riskLevelVO.setRiskEnum(sceneRisk);
        } else {
            //场景风险低于圈复杂度风险
            riskLevelVO.setDecision(complexity + ":" + complexityRisk.getComplexityValue() + ";" + sceneFlow + ":" + flow);
            riskLevelVO.setRiskEnum(complexityRisk.getRiskEnum());
        }
        riskLevelVO.setComplexityValue(complexityRisk.getComplexityValue());
        return riskLevelVO;
    }

    /**
     * 获取场景风险等级
     *
     * <p>根据应用名称和场景流量，评估场景的业务风险等级。该方法负责：</p>
     * <ul>
     *   <li>查询应用的场景流量分位值配置</li>
     *   <li>根据流量阈值判断风险等级</li>
     *   <li>流量高于低分位值：高风险</li>
     *   <li>流量在高、低分位值之间：中风险</li>
     *   <li>流量低于高分位值：低风险</li>
     *   <li>无流量数据或配置缺失：未知风险</li>
     * </ul>
     *
     * <p>场景风险评估基于真实的业务流量数据，反映业务影响程度。</p>
     *
     * @param app 应用名称，指定要评估的应用系统
     * @param flow 场景流量，表示该场景被访问的频率
     * @return RiskEnum 返回场景风险等级，包含高风险、中风险、低风险和未知风险
     */
    private RiskEnum getSceneRisk(String app, int flow) {
        RiskEnum risk = RiskEnum.UNKNOWRISK;
        //获取场景风险分位值
        if (flow > 0) {
            SceneRiskLevel sceneRiskLevel = sceneRiskLevelMapper.queryObject(app);
            if (null != sceneRiskLevel) {
                if (flow >= sceneRiskLevel.getLowerQuantile()) {
                    risk = RiskEnum.HIGHRISK;
                } else if (flow >= sceneRiskLevel.getHigherQuantile() && flow < sceneRiskLevel.getLowerQuantile()) {
                    //风险就高
                    risk = RiskEnum.MEDIUMRISK;
                } else {
                    risk = RiskEnum.LOWRISK;
                }
            }
        }
        return risk;
    }

    /**
     * 获取圈复杂度风险等级
     *
     * <p>根据应用名称和方法名，评估方法的圈复杂度技术风险。该方法负责：</p>
     * <ul>
     *   <li>查询方法的圈复杂度数值</li>
     *   <li>根据配置的复杂度阈值判断风险等级</li>
     *   <li>复杂度高于高分位值：高风险</li>
     *   <li>复杂度在高、低分位值之间：中风险</li>
     *   <li>复杂度低于低分位值：低风险</li>
     *   <li>无复杂度数据：未知风险</li>
     *   <li>返回包含复杂度数值的详细评估结果</li>
     * </ul>
     *
     * <p>圈复杂度评估反映了代码的维护难度和出错概率，是技术债务的重要指标。</p>
     *
     * @param app 应用名称，指定要评估的应用系统
     * @param methodName 方法名称，指定要评估的具体方法
     * @return RiskLevelVO 返回圈复杂度风险评估结果，包含风险等级和复杂度数值
     */
    private RiskLevelVO getComplexityRisk(String app, String methodName) {
        RiskLevelVO riskLevelVO = new RiskLevelVO();
        RiskEnum risk = RiskEnum.UNKNOWRISK;
        //获取方法圈复杂度
        MethodComplexity complexity = complexityMapper.getValueByMethodName(app, methodName);
        int value = 0;
        if (complexity != null) {
            value = complexity.getValue();
            if (value >= cxtyHigherQuantile) {
                risk = RiskEnum.HIGHRISK;
            } else if (value < cxtyHigherQuantile && value >= cxtyLowerQuantile) {
                //风险就高
                risk = RiskEnum.MEDIUMRISK;
            } else {
                risk = RiskEnum.LOWRISK;
            }
        }
        riskLevelVO.setRiskEnum(risk);
        riskLevelVO.setComplexityValue(value);
        return riskLevelVO;
    }

    /**
     * 获取服务的高风险定义规则说明
     *
     * <p>根据服务名称，生成该服务的风险评估规则说明。该方法负责：</p>
     * <ul>
     *   <li>查询服务的场景流量风险分位值配置</li>
     *   <li>生成场景流量的风险评估阈值说明</li>
     *   <li>生成圈复杂度的风险评估阈值说明</li>
     *   <li>提供统一的风险等级定义和规则说明</li>
     *   <li>返回格式化的规则文本，便于理解和展示</li>
     * </ul>
     *
     * <p>规则说明用于指导开发人员理解风险评估标准，辅助代码审查和发布决策。</p>
     *
     * @param serviceName 服务名称，指定要查询规则的应用系统
     * @return String 返回风险评估规则说明文本，包含场景流量和圈复杂度的风险阈值定义
     */
    public String getServiceRule(String serviceName) {

        SceneRiskLevel serviceQuantile = sceneRiskLevelMapper.queryObject(serviceName);
        return "\t\t\t\t\t\t\t\t\t\t\t\t\t高风险容易出故障；中风险容易出线上问题\n核心场景:高风险       " + (null == serviceQuantile ? "圈复杂度:高风险>=" + cxtyHigherQuantile + ">中风险>=" + cxtyLowerQuantile + ">" + "低风险" :
                "场景流量:高风险>=" + serviceQuantile.getLowerQuantile() + ">中风险>=" + serviceQuantile.getHigherQuantile() + ">" + "低风险       " +
                        "圈复杂度:高风险>=" + cxtyHigherQuantile + ">中风险>=" + cxtyLowerQuantile + ">" + "低风险");
    }
}