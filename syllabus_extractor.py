#!/usr/bin/env python3
"""
Syllabus Schedule Extraction System - Complete Implementation
Extract schedule from PDF syllabi and generate calendar variants.

Usage:
    python syllabus_extractor.py

Requirements:
    pip install pdfplumber dateparser icalendar python-dateutil
"""

import json
import pdfplumber
from datetime import datetime, timedelta
from dateparser.search import search_dates
from icalendar import Calendar, Event
from dateutil.rrule import MO, TU, WE, TH, FR, SA, SU
from itertools import product

# =============================================================================
# CONFIGURATION
# =============================================================================

# Modify these for your semester
SEMESTER_START = datetime(2026, 8, 24, 0, 0)
SEMESTER_END = datetime(2026, 12, 10, 23, 59)

# Input files
SYLLABUS_PDF = "syllabus_412.pdf"
USER_CONFIG = "user_config.json"
OUTPUT_BASE = "schedule"

# =============================================================================
# PDF EXTRACTION
# =============================================================================

def extract_schedule_from_pdf(pdf_path):
    """Extract schedule events from syllabus PDF."""
    events = []
    
    with pdfplumber.open(pdf_path) as pdf:
        for page in pdf.pages:
            # Extract tables (most common in syllabi)
            tables = page.extract_tables()
            
            for table in tables:
                if not table or len(table) < 2:
                    continue
                
                # Check if this is a schedule table
                header = [str(c).lower() if c else "" for c in table[0]]
                if not any(kw in ' '.join(header) for kw in ['date', 'week', 'assignment', 'due']):
                    continue
                
                # Find column indices
                date_col = next((i for i, h in enumerate(header) if 'date' in h or 'week' in h), 0)
                topic_col = next((i for i, h in enumerate(header) if 'topic' in h), 1)
                assign_col = next((i for i, h in enumerate(header) if 'assignment' in h or 'due' in h), -1)
                
                # Parse rows
                for row in table[1:]:
                    if len(row) <= date_col or not row[date_col]:
                        continue
                    
                    # Parse date
                    dates = search_dates(str(row[date_col]))
                    if not dates:
                        continue
                    
                    # Extract info
                    topic = str(row[topic_col]) if len(row) > topic_col and row[topic_col] else ""
                    assignment = str(row[assign_col]) if assign_col >= 0 and len(row) > assign_col and row[assign_col] else ""
                    
                    # Classify event
                    text = f"{topic} {assignment}".lower()
                    if any(w in text for w in ['exam', 'midterm', 'final', 'test']):
                        event_type = 'Exam'
                    elif any(w in text for w in ['assignment', 'homework', 'hw', 'pset']):
                        event_type = 'Assignment'
                    elif 'quiz' in text:
                        event_type = 'Quiz'
                    elif any(w in text for w in ['project', 'presentation']):
                        event_type = 'Project'
                    else:
                        event_type = 'Class'
                    
                    events.append({
                        'date': dates[0][1],
                        'type': event_type,
                        'description': topic.strip(),
                        'assignment': assignment.strip()
                    })
    
    return events

# =============================================================================
# USER EVENT CREATION
# =============================================================================

def create_user_events(config, start_date, end_date):
    """Create user-defined events from config."""
    events = []
    day_map = {'monday': MO, 'tuesday': TU, 'wednesday': WE, 'thursday': TH, 'friday': FR}
    
    # Rest blocks
    if 'rest' in config:
        sleep_time = config['rest'].get('default_sleep_time', '23:00')
        duration = config['rest'].get('duration_hours', 8)
        hour, minute = map(int, sleep_time.split(':'))
        endTime = hour + duration
        
        event = Event()
        event.add('summary', 'Rest/Sleep')
        event.add('dtstart', start_date.replace(hour=hour, minute=minute))
        if(endTime > 23):
            event.add('dtend', start_date.replace(day=start_date.day+1,hour=endTime-23, minute=minute))
        else:
            event.add('dtend', start_date.replace(hour=hour+duration, minute=minute))
        event.add('rrule', {'freq': 'daily', 'until': end_date})
        event.add('categories', ['Personal'])
        events.append(event)
    
    # Office hours
    if 'office_hours' in config:
        for oh in config['office_hours']:
            start_h, start_m = map(int, oh['start_time'].split(':'))
            end_h, end_m = map(int, oh['end_time'].split(':'))
            days = [day_map[d.lower()] for d in oh['days']]
            
            event = Event()
            event.add('summary', f"Office Hours - {oh['professor']}")
            event.add('dtstart', start_date.replace(hour=start_h, minute=start_m))
            event.add('dtend', start_date.replace(hour=end_h, minute=end_m))
            event.add('location', oh.get('location', ''))
            event.add('rrule', {'freq': 'weekly', 'byday': days, 'until': end_date})
            events.append(event)
    
    # Personal events
    if 'personal_events' in config:
        for pe in config['personal_events']:
            event = Event()
            event.add('summary', pe['summary'])
            
            if pe.get('recurring'):
                start_h, start_m = map(int, pe['start_time'].split(':'))
                end_h, end_m = map(int, pe['end_time'].split(':'))
                days = [day_map[d.lower()] for d in pe['days']]
                
                event.add('dtstart', start_date.replace(hour=start_h, minute=start_m))
                event.add('dtend', start_date.replace(hour=end_h, minute=end_m))
                event.add('rrule', {'freq': 'weekly', 'byday': days, 'until': end_date})
            else:
                from dateparser import parse
                event.add('dtstart', parse(pe['start']))
                event.add('dtend', parse(pe['end']))
            
            if pe.get('location'):
                event.add('location', pe['location'])
            events.append(event)
    
    return events

# =============================================================================
# VARIANT GENERATION
# =============================================================================

def generate_variants(syllabus_events, config, start_date, end_date, max_variants=5):
    """Generate multiple schedule variants."""
    sleep_options = config.get('rest', {}).get('sleep_time_options', ['23:00'])
    variants = []
    
    for sleep_time in sleep_options:
        print(sleep_time)
        print(sleep_time.split(':')[0])
        # Create events with this sleep time
        variant_events = syllabus_events.copy()
        
        # Modify config for this variant
        variant_config = config.copy()
        if 'rest' not in variant_config:
            variant_config['rest'] = {}
        variant_config['rest']['default_sleep_time'] = sleep_time
        
        # Add user events
        user_events = create_user_events(variant_config, start_date, end_date)
        variant_events.extend(user_events)
        
        # Score variant
        score = 100.0
        sleep_hour = int(sleep_time.split(':')[0])
        if sleep_hour >= 2 or sleep_hour == 0:
            score -= 15
        elif sleep_hour >= 1:
            score -= 10
        elif sleep_hour == config.get('preferences', {}).get('preferred_sleep_hour', 23):
            score += 5
        
        variants.append({
            'events': variant_events,
            'sleep_time': sleep_time,
            'score': score
        })
    
    variants.sort(key=lambda v: v['score'], reverse=True)
    return variants[:max_variants]

# =============================================================================
# CALENDAR EXPORT
# =============================================================================

def create_calendar(events, name="Schedule"):
    """Create iCalendar from events."""
    cal = Calendar()
    cal.add('prodid', f'-//{name}//EN')
    cal.add('version', '2.0')
    cal.add('x-wr-calname', name)
    
    for e in events:
        if isinstance(e, Event):
            cal.add_component(e)
        else:
            event = Event()
            summary = e['type']
            if e.get('description'):
                summary += f": {e['description']}"
            if e.get('assignment'):
                summary += f" | {e['assignment']}"
            
            event.add('summary', summary)
            event.add('dtstart', e['date'])
            event.add('dtend', e['date'] + timedelta(hours=1))
            event.add('categories', [e['type']])
            cal.add_component(event)
    
    return cal

def save_calendar(calendar, filename):
    """Save calendar to .ics file."""
    with open(filename, 'wb') as f:
        f.write(calendar.to_ical())

# =============================================================================
# MAIN WORKFLOW
# =============================================================================

def main():
    """Main execution."""
    print("=" * 70)
    print("SYLLABUS SCHEDULE EXTRACTION SYSTEM")
    print("=" * 70)
    
    # Extract syllabus
    print("\n[1/4] Extracting syllabus events...")
    try:
        syllabus_events = extract_schedule_from_pdf(SYLLABUS_PDF)
        print(f"  ✓ Found {len(syllabus_events)} events")
    except Exception as e:
        print(f"  ✗ Error: {e}")
        syllabus_events = []
    
    # Load config
    print("\n[2/4] Loading user preferences...")
    try:
        with open(USER_CONFIG, 'r') as f:
            config = json.load(f)
        print("  ✓ Configuration loaded")
    except Exception as e:
        print(f"  ✗ Error: {e}")
        config = {}
    
    # Generate variants
    print("\n[3/4] Generating schedule variants...")
    try:
        variants = generate_variants(syllabus_events, config, SEMESTER_START, SEMESTER_END)
        print(f"  ✓ Generated {len(variants)} variants")
        
        for i, v in enumerate(variants, 1):
            print(f"    Variant {i}: Sleep {v['sleep_time']} (Score: {v['score']:.1f})")
    except Exception as e:
        print(f"  ✗ Error: {e}")
        variants = []
    
    # Export calendars
    print("\n[4/4] Exporting calendars...")
    try:
        for i, variant in enumerate(variants, 1):
            cal = create_calendar(variant['events'], f"Schedule Variant {i}")
            filename = f"{OUTPUT_BASE}_variant_{i}.ics"
            save_calendar(cal, filename)
            print(f"  ✓ {filename}")
    except Exception as e:
        print(f"  ✗ Error: {e}")
    
    print("\n" + "=" * 70)
    print("COMPLETE!")
    print("Import .ics files into Google Calendar to compare variants.")
    print("=" * 70)

# =============================================================================
# EXAMPLE CONFIGURATION GENERATOR
# =============================================================================

def create_example_config():
    """Create example user_config.json file."""
    config = {
        "rest": {
            "default_sleep_time": "23:00",
            "sleep_time_options": ["22:00", "23:00", "00:00", "01:00"],
            "duration_hours": 8
        },
        "office_hours": [
            {
                "professor": "Dr. Smith",
                "days": ["Monday", "Wednesday"],
                "start_time": "14:00",
                "end_time": "16:00",
                "location": "Room 301"
            }
        ],
        "personal_events": [
            {
                "summary": "Gym",
                "recurring": True,
                "days": ["Monday", "Wednesday", "Friday"],
                "start_time": "06:00",
                "end_time": "07:00",
                "location": "Campus Gym"
            }
        ],
        "preferences": {
            "preferred_sleep_hour": 23
        }
    }
    
    with open('user_config.json', 'w') as f:
        json.dump(config, f, indent=2)
    
    print("Created example user_config.json")

# =============================================================================
# ENTRY POINT
# =============================================================================

if __name__ == "__main__":
    import sys
    
    if len(sys.argv) > 1 and sys.argv[1] == "--create-config":
        create_example_config()
    else:
        main()
