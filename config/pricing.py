"""Data Detective AI pricing configuration.

Prices are display/configuration values only. No payment processor is connected.
All amounts are USD. Do not treat a paid plan as proof of payment.
"""

CURRENCY = "USD"

PLANS = {
    "one_visit": {
        "name": "One Visit",
        "price": 1.00,
        "billing_period": "one_time",
        "description": "One paid visit/session.",
    },
    "monthly_pro": {
        "name": "Monthly Pro",
        "price": 5.00,
        "billing_period": "month",
        "description": "Pro access for one month.",
    },
    "yearly_pro": {
        "name": "Yearly Pro",
        "price": 50.00,
        "billing_period": "year",
        "description": "Pro access for one year.",
    },
}

FOUNDER_PLAN = {
    "name": "Founder Premium",
    "price": 0.00,
    "billing_period": "lifetime",
    "description": "Founder access; external provider usage may still have costs or limits.",
}


def get_public_plans():
    """Return safe plan data for display on a pricing page."""
    return {
        "currency": CURRENCY,
        "plans": PLANS,
        "founder": FOUNDER_PLAN,
    }


def is_founder(username, configured_founder_username):
    """Compare a logged-in username to the configured founder username.

    Authentication must happen elsewhere. Never use this function alone to
    authenticate a user or hard-code a founder password in source code.
    """
    if not username or not configured_founder_username:
        return False
    return username.strip().casefold() == configured_founder_username.strip().casefold()
