"""Modular agent directory for Data Detective AI.

These modules describe and route tasks. They do not claim to generate media
or provide LLM intelligence until a real provider is configured.
"""

from .registry import AGENTS, get_agent, route_task

__all__ = ["AGENTS", "get_agent", "route_task"]
