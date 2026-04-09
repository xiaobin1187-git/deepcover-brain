#!/usr/bin/env python3
"""
高并发压力测试脚本
模拟10000个客户端同时发起连接请求，用于测试服务器的并发处理能力
"""

import asyncio
import aiohttp
import time
import argparse
from typing import List, Dict, Any
from dataclasses import dataclass
from datetime import datetime, timedelta
import statistics
import signal
import sys


@dataclass
class TestConfig:
    """测试配置"""
    target_url: str
    total_clients: int = 10000
    concurrent_limit: int = 100  # 同时并发数限制
    request_timeout: int = 30  # 请求超时时间（秒）
    method: str = "GET"
    headers: Dict[str, str] = None
    body: str = None
    test_duration: int = 60  # 测试持续时间（秒）


@dataclass
class TestResult:
    """测试结果统计"""
    total_requests: int = 0
    successful_requests: int = 0
    failed_requests: int = 0
    timeout_requests: int = 0
    response_times: List[float] = None
    errors: Dict[str, int] = None
    start_time: float = 0
    end_time: float = 0

    def __post_init__(self):
        if self.response_times is None:
            self.response_times = []
        if self.errors is None:
            self.errors = {}


class LoadTestRunner:
    """压力测试运行器"""

    def __init__(self, config: TestConfig):
        self.config = config
        self.result = TestResult()
        self.is_running = True
        self.semaphore = asyncio.Semaphore(config.concurrent_limit)

        # 注册信号处理，优雅退出
        signal.signal(signal.SIGINT, self._signal_handler)
        signal.signal(signal.SIGTERM, self._signal_handler)

    def _signal_handler(self, signum, frame):
        """处理中断信号"""
        print("\n\n收到中断信号，正在停止测试...")
        self.is_running = False

    async def single_request(self, session: aiohttp.ClientSession, client_id: int) -> Dict[str, Any]:
        """
        执行单个请求

        Args:
            session: aiohttp会话
            client_id: 客户端ID

        Returns:
            请求结果字典
        """
        async with self.semaphore:  # 限制并发数
            if not self.is_running:
                return {"success": False, "error": "test_stopped"}

            start_time = time.time()
            result = {
                "client_id": client_id,
                "success": False,
                "error": None,
                "status_code": None,
                "response_time": 0
            }

            try:
                # 根据请求方法发送请求
                if self.config.method.upper() == "GET":
                    async with session.get(
                        self.config.target_url,
                        timeout=aiohttp.ClientTimeout(total=self.config.request_timeout)
                    ) as response:
                        result["status_code"] = response.status
                        result["success"] = 200 <= response.status < 300
                        await response.read()  # 确保读取完整响应

                elif self.config.method.upper() == "POST":
                    async with session.post(
                        self.config.target_url,
                        data=self.config.body,
                        headers=self.config.headers,
                        timeout=aiohttp.ClientTimeout(total=self.config.request_timeout)
                    ) as response:
                        result["status_code"] = response.status
                        result["success"] = 200 <= response.status < 300
                        await response.read()

                else:
                    raise ValueError(f"不支持的HTTP方法: {self.config.method}")

                result["response_time"] = time.time() - start_time

            except asyncio.TimeoutError:
                result["error"] = "timeout"
                result["response_time"] = self.config.request_timeout
            except aiohttp.ClientError as e:
                result["error"] = f"client_error: {type(e).__name__}"
            except Exception as e:
                result["error"] = f"unexpected_error: {type(e).__name__}"

            return result

    async def run_client_batch(self, session: aiohttp.ClientSession, start_id: int, count: int) -> List[Dict]:
        """
        运行一批客户端请求

        Args:
            session: aiohttp会话
            start_id: 起始客户端ID
            count: 客户端数量

        Returns:
            该批次所有请求的结果
        """
        tasks = []
        for i in range(count):
            client_id = start_id + i
            task = self.single_request(session, client_id)
            tasks.append(task)

        # 并发执行所有请求
        results = await asyncio.gather(*tasks, return_exceptions=True)

        # 处理异常结果
        processed_results = []
        for result in results:
            if isinstance(result, Exception):
                processed_results.append({
                    "success": False,
                    "error": f"task_exception: {type(result).__name__}"
                })
            else:
                processed_results.append(result)

        return processed_results

    def print_progress(self, completed: int, total: int):
        """打印进度"""
        progress = completed / total * 100
        print(f"\r进度: {completed}/{total} ({progress:.1f}%)", end="", flush=True)

    async def run_test(self):
        """运行压力测试"""
        print(f"{'='*60}")
        print(f"压力测试开始")
        print(f"目标URL: {self.config.target_url}")
        print(f"客户端数量: {self.config.total_clients}")
        print(f"并发限制: {self.config.concurrent_limit}")
        print(f"请求方法: {self.config.method}")
        print(f"{'='*60}\n")

        self.result.start_time = time.time()

        # 创建HTTP连接池
        connector = aiohttp.TCPConnector(
            limit=self.config.concurrent_limit,  # 连接池大小
            limit_per_host=self.config.concurrent_limit,  # 每个主机的连接数
            ttl_dns_cache=300,  # DNS缓存时间
        )

        timeout = aiohttp.ClientTimeout(total=self.config.request_timeout)

        async with aiohttp.ClientSession(
            connector=connector,
            timeout=timeout
        ) as session:
            # 将总客户端数分成多个批次
            batch_size = self.config.concurrent_limit
            num_batches = (self.config.total_clients + batch_size - 1) // batch_size

            print(f"开始发起 {self.config.total_clients} 个请求（分 {num_batches} 个批次）\n")

            for batch_idx in range(num_batches):
                if not self.is_running:
                    break

                start_id = batch_idx * batch_size
                count = min(batch_size, self.config.total_clients - start_id)

                # 执行当前批次
                batch_results = await self.run_client_batch(session, start_id, count)

                # 统计结果
                for result in batch_results:
                    self.result.total_requests += 1

                    if result["success"]:
                        self.result.successful_requests += 1
                        if "response_time" in result:
                            self.result.response_times.append(result["response_time"])
                    else:
                        self.result.failed_requests += 1
                        error_type = result.get("error", "unknown")
                        self.result.errors[error_type] = self.result.errors.get(error_type, 0) + 1

                        if error_type == "timeout":
                            self.result.timeout_requests += 1

                # 打印进度
                completed = (batch_idx + 1) * batch_size
                self.print_progress(min(completed, self.config.total_clients), self.config.total_requests)

        self.result.end_time = time.time()
        print()  # 换行

    def print_results(self):
        """打印测试结果统计"""
        duration = self.result.end_time - self.result.start_time
        qps = self.result.total_requests / duration if duration > 0 else 0

        print(f"\n{'='*60}")
        print(f"压力测试结果")
        print(f"{'='*60}")
        print(f"总请求数:        {self.result.total_requests}")
        print(f"成功请求:        {self.result.successful_requests} ({self.result.successful_requests/self.result.total_requests*100:.1f}%)")
        print(f"失败请求:        {self.result.failed_requests} ({self.result.failed_requests/self.result.total_requests*100:.1f}%)")
        print(f"超时请求:        {self.result.timeout_requests}")
        print(f"测试时长:        {duration:.2f} 秒")
        print(f"QPS (每秒请求数): {qps:.2f}")
        print(f"{'='*60}")

        # 响应时间统计
        if self.result.response_times:
            print(f"\n响应时间统计:")
            print(f"  平均响应时间:  {statistics.mean(self.result.response_times)*1000:.2f} ms")
            print(f"  中位数响应时间: {statistics.median(self.result.response_times)*1000:.2f} ms")
            if len(self.result.response_times) > 1:
                print(f"  标准差:        {statistics.stdev(self.result.response_times)*1000:.2f} ms")
            print(f"  最小响应时间:  {min(self.result.response_times)*1000:.2f} ms")
            print(f"  最大响应时间:  {max(self.result.response_times)*1000:.2f} ms")

            # 百分位数
            if len(self.result.response_times) >= 10:
                sorted_times = sorted(self.result.response_times)
                p90 = sorted_times[int(len(sorted_times) * 0.9)]
                p95 = sorted_times[int(len(sorted_times) * 0.95)]
                p99 = sorted_times[int(len(sorted_times) * 0.99)]
                print(f"  P90:           {p90*1000:.2f} ms")
                print(f"  P95:           {p95*1000:.2f} ms")
                print(f"  P99:           {p99*1000:.2f} ms")

        # 错误统计
        if self.result.errors:
            print(f"\n错误类型统计:")
            for error_type, count in sorted(self.result.errors.items(), key=lambda x: x[1], reverse=True):
                print(f"  {error_type}: {count}")

        print(f"{'='*60}\n")


async def main():
    """主函数"""
    parser = argparse.ArgumentParser(description="高并发压力测试工具")
    parser.add_argument("url", help="目标URL（例如: http://localhost:8080/api/test）")
    parser.add_argument("-n", "--number", type=int, default=10000, help="总客户端数量（默认: 10000）")
    parser.add_argument("-c", "--concurrent", type=int, default=100, help="并发连接数（默认: 100）")
    parser.add_argument("-t", "--timeout", type=int, default=30, help="请求超时时间（秒，默认: 30）")
    parser.add_argument("-m", "--method", default="GET", choices=["GET", "POST"], help="HTTP方法（默认: GET）")
    parser.add_argument("-b", "--body", help="POST请求体")
    parser.add_argument("--header", action="append", help="添加HTTP头（格式: 'Key: Value'）")

    args = parser.parse_args()

    # 构建请求头
    headers = {}
    if args.header:
        for header in args.header:
            if ":" in header:
                key, value = header.split(":", 1)
                headers[key.strip()] = value.strip()

    # 创建测试配置
    config = TestConfig(
        target_url=args.url,
        total_clients=args.number,
        concurrent_limit=args.concurrent,
        request_timeout=args.timeout,
        method=args.method,
        headers=headers if headers else None,
        body=args.body
    )

    # 运行测试
    runner = LoadTestRunner(config)
    await runner.run_test()
    runner.print_results()


if __name__ == "__main__":
    # 检查Python版本
    if sys.version_info < (3, 7):
        print("错误: 需要Python 3.7或更高版本")
        sys.exit(1)

    # 运行异步主函数
    asyncio.run(main())
